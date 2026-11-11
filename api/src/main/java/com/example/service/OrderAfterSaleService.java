package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.OrderAfterSaleMapper;
import com.example.mapper.OrderMapper;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Order;
import com.example.pojo.entity.OrderAfterSale;
import com.example.pojo.vo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderAfterSaleService {
    private final OrderMapper orders;
    private final OrderAfterSaleMapper requests;
    private final OrderStockReturnService stockReturns;
    private final ObjectMapper json;

    public void enrich(OrderVO order) {
        if (order == null) return;
        var history = requests.history(order.getId());
        order.setAfterSaleHistory(history);
        if (!history.isEmpty()) order.setAfterSale(history.getFirst());
        String eligibility = eligibility(order.getOrderStatus(), order.getReceiveTime(), order.getAfterSaleDeadline(), history);
        order.setAfterSaleEligibility(eligibility);
        order.setAfterSaleMessage(eligibilityMessage(order.getOrderStatus(), order.getReceiveTime(), eligibility));
    }

    public AfterSaleContextVO read(Integer orderId, Integer actor, String role) {
        Order order = orders.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在",404);
        String viewer = viewer(order,actor,role);
        var history = requests.history(orderId);
        String eligibility = eligibility(order.getOrderStatus(),order.getReceiveTime(),order.getAfterSaleDeadline(),history);
        return new AfterSaleContextVO(orderId,order.getOrderStatus(),order.getQuantity(),viewer,eligibility,
                eligibilityMessage(order.getOrderStatus(),order.getReceiveTime(),eligibility),
                order.getAfterSaleDeadline(),OrderTime.now(),history);
    }

    private String viewer(Order order, Integer actor, String role) {
        if (actor == null) throw new BusinessException("请先登录",401);
        if ("ADMIN".equals(role)) return "ADMIN";
        if (Objects.equals(order.getBuyerId(),actor)) return "BUYER";
        if (Objects.equals(order.getSellerId(),actor)) return "SELLER";
        throw new BusinessException("只有本单买卖双方和管理员可以查看售后",403);
    }

    private String eligibility(Integer status,LocalDateTime received,LocalDateTime deadline,List<OrderAfterSale> history) {
        if (history.stream().anyMatch(r->AfterSalePolicy.ACTIVE.contains(r.getState()))) return "PROCESSING";
        if (!Integer.valueOf(2).equals(status) && !Integer.valueOf(3).equals(status)) return "UNAVAILABLE";
        if (!history.isEmpty() && "REJECTED".equals(history.getFirst().getState())) return "ADMIN_ONLY";
        return AfterSalePolicy.regular(status,received,deadline,OrderTime.now()) ? "REGULAR" : "ADMIN_ONLY";
    }

    private String eligibilityMessage(Integer status,LocalDateTime received,String eligibility) {
        return switch(eligibility) {
            case "PROCESSING" -> "已有售后申请正在处理，请查看当前进度";
            case "REGULAR" -> Integer.valueOf(3).equals(status) ? "已完成交易，当前仍在七天售后期内" : "尚未确认收货，可以申请售后";
            case "ADMIN_ONLY" -> received == null ? "历史完成时间缺失，请申请管理员介入核对" : "普通售后入口已关闭，可说明原因申请管理员介入";
            default -> Integer.valueOf(4).equals(status) ? "订单已结束，可查看处理记录" : "当前订单状态暂不能申请售后";
        };
    }

    public void requireNoActive(Integer orderId) {
        if (requests.activeCount(orderId)>0) throw new BusinessException("该订单正在处理售后，请先完成或撤回售后申请",409);
    }

    @Transactional(rollbackFor=Exception.class)
    public void apply(Integer actor,Integer orderId,AfterSaleApplyDTO dto) {
        var order = lockOrder(orderId);
        if (!Objects.equals(order.getBuyerId(),actor)) throw new BusinessException("只能申请自己的购买订单",403);
        var history=requests.history(orderId);
        String eligibility=eligibility(order.getOrderStatus(),order.getReceiveTime(),order.getAfterSaleDeadline(),history);
        if ("PROCESSING".equals(eligibility)) throw new BusinessException("已有正在处理的售后申请，请勿重复提交",409);
        if ("UNAVAILABLE".equals(eligibility)) throw new BusinessException("当前订单状态不允许申请售后",409);
        if ("REGULAR".equals(dto.getRequestType()) && !"REGULAR".equals(eligibility)) {
            throw new BusinessException("已超过普通售后期限或需要核对历史记录，请选择管理员介入",409);
        }
        if (!Set.of("REGULAR","ADMIN_INTERVENTION").contains(dto.getRequestType())) throw new BusinessException("申请方式无效",400);
        List<String> images=dto.getEvidence()==null?List.of():dto.getEvidence();
        if (images.size()>3 || new HashSet<>(images).size()!=images.size()) throw new BusinessException("凭证图片最多3张且不能重复",400);
        for (String path:images) if(path==null || !path.matches("^/files/products/"+actor+"/[a-zA-Z0-9_-]+\\.(png|jpe?g)$")) {
            throw new BusinessException("凭证图片需由申请人通过本站上传",400);
        }
        var r=new OrderAfterSale();
        r.setOrderId(orderId);r.setApplicantId(actor);r.setRequestType(dto.getRequestType());
        r.setReason(text(dto.getReason(),"申请原因"));r.setReturnRequired(dto.getReturnRequired());
        r.setOriginalOrderStatus(order.getOrderStatus());r.setState("REGULAR".equals(dto.getRequestType())?"WAIT_SELLER":"WAIT_ADMIN");
        try { r.setEvidenceJson(json.writeValueAsString(images)); } catch(JsonProcessingException e) { throw new BusinessException("凭证信息无法保存，请重试",400); }
        r.setCreatedAt(OrderTime.now());r.setUpdatedAt(r.getCreatedAt());
        if(requests.insert(r)!=1)throw new BusinessException("申请未保存，请重试",409);
    }

    @Transactional(rollbackFor=Exception.class)
    public void sellerRespond(Integer actor,Integer id,AfterSaleResponseDTO dto) {
        var pair=lockRequest(id);var order=pair.order();var r=pair.request();
        if(!Objects.equals(order.getSellerId(),actor))throw new BusinessException("只能回应自己出售商品的售后申请",403);
        if (!"WAIT_SELLER".equals(r.getState())
                && !("WAIT_ADMIN".equals(r.getState()) && r.getSellerRespondedAt()==null)) {
            throw new BusinessException("售后状态已变化或卖家意见已提交，请刷新后重试",409);
        }
        if(dto.getAgree()==null)throw new BusinessException("请选择同意或拒绝",400);
        r.setSellerAgree(dto.getAgree());r.setSellerNote(text(dto.getNote(),"卖家处理说明"));
        r.setSellerId(actor);r.setSellerRespondedAt(OrderTime.now());r.setState("WAIT_ADMIN");save(r);
    }

    @Transactional(rollbackFor=Exception.class)
    public void review(Integer admin,Integer id,AfterSaleReviewDTO dto) {
        var pair=lockRequest(id);var order=pair.order();var r=pair.request();
        requireState(r,"WAIT_ADMIN");
        if(dto.getApproved()==null)throw new BusinessException("请选择审核结果",400);
        r.setAdminId(admin);r.setAdminNote(text(dto.getNote(),"审核说明"));r.setAdminReviewedAt(OrderTime.now());
        if(dto.getApproved()) {
            if(dto.getReturnRequired()==null)throw new BusinessException("请确认是否需要退回商品",400);
            r.setReturnRequired(dto.getReturnRequired());
            if(dto.getReturnRequired()) r.setState("WAIT_RETURN");
            else {
                r.setState("APPROVED");r.setReturnQuantity(0);r.setStockCreditedQuantity(0);r.setStockDisposition("NO_RETURN");
                order.setStockReleaseDisposition("NO_RETURN");order.setStockDeductedQuantity(0);finish(order,r);
            }
        } else {
            r.setState("REJECTED");
            if(Integer.valueOf(5).equals(order.getOrderStatus())) {
                Integer original=r.getOriginalOrderStatus()!=null?r.getOriginalOrderStatus():dto.getOriginalOrderStatus();
                if(original==null || (original!=2&&original!=3))throw new BusinessException("请核对历史订单原交易状态后再驳回",400);
                r.setOriginalOrderStatus(original);order.setOrderStatus(original);updateOrder(order);
            }
        }
        save(r);
    }

    @Transactional(rollbackFor=Exception.class)
    public void confirmReturn(Integer actor,String role,Integer id,AfterSaleReturnDTO dto) {
        var pair=lockRequest(id);var order=pair.order();var r=pair.request();
        if(!"ADMIN".equals(role)&&!Objects.equals(order.getSellerId(),actor))throw new BusinessException("只有卖家或管理员可以确认收到退回商品",403);
        requireState(r,"WAIT_RETURN");
        r.setReturnNote(text(dto.getNote(),"退回确认说明"));
        if(order.getQuantity()==null||order.getQuantity()<1)throw new BusinessException("历史购买数量缺失，请先核对订单",409);
        r.setStockCreditedQuantity(stockReturns.release(order,true));
        r.setStockDisposition(order.getStockReleaseDisposition());r.setReturnQuantity(order.getQuantity());
        r.setReturnConfirmedBy(actor);r.setReturnConfirmedAt(OrderTime.now());r.setState("APPROVED");
        finish(order,r);save(r);
    }

    @Transactional(rollbackFor=Exception.class)
    public void withdraw(Integer actor,Integer id) {
        var pair=lockRequest(id);var r=pair.request();
        if(!Objects.equals(pair.order().getBuyerId(),actor))throw new BusinessException("只能撤回自己的售后申请",403);
        if(!Set.of("WAIT_SELLER","WAIT_ADMIN").contains(r.getState()))throw new BusinessException("当前售后阶段不能撤回",409);
        if("LEGACY".equals(r.getRequestType()))throw new BusinessException("历史售后需要管理员核对，不能直接撤回",409);
        r.setState("WITHDRAWN");save(r);
    }

    private void finish(Order order,OrderAfterSale r) {
        order.setOrderStatus(4);order.setCancelTime(OrderTime.now());
        order.setCancelReason("售后审核通过并完成，申请编号："+r.getId());updateOrder(order);
    }
    private void updateOrder(Order order) {
        if(orders.updateById(order)!=1)throw new BusinessException("订单发生变化，请刷新后重试",409);
    }
    private void save(OrderAfterSale r) {
        r.setUpdatedAt(OrderTime.now());
        if(requests.updateById(r)!=1)throw new BusinessException("售后记录发生变化，请刷新后重试",409);
    }
    private Order lockOrder(Integer id) {
        Order order=orders.selectForUpdate(id);
        if(order==null)throw new BusinessException("订单不存在",404);
        return order;
    }
    private record Locked(Order order,OrderAfterSale request) {}
    private Locked lockRequest(Integer id) {
        var initial=requests.selectById(id);if(initial==null)throw new BusinessException("售后申请不存在",404);
        // 所有状态操作采用相同锁顺序：订单 -> 售后记录 -> 商品。
        var order=lockOrder(initial.getOrderId());var r=requests.lock(id);
        if(r==null)throw new BusinessException("售后申请不存在",404);
        return new Locked(order,r);
    }
    private void requireState(OrderAfterSale r,String state) {
        if(!state.equals(r.getState()))throw new BusinessException("售后状态已变化，请刷新后重试",409);
    }
    private String text(String value,String label) {
        if(value==null||value.isBlank()||value.length()>1000)throw new BusinessException("请填写"+label+"，最多1000字",400);
        return value.trim();
    }
}
