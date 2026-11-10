package com.example.service;

import com.example.exception.BusinessException;
import com.example.pojo.vo.OrderVO;
import com.example.pojo.vo.OrderOverviewVO;
import com.example.pojo.vo.OrderOverviewVO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class OrderOverviewService {
    private final OrderService orders;
    public OrderOverviewVO read(Integer id,Integer viewer) {
        if(id==null || id<=0) throw new BusinessException("订单编号无效",400);
        if(viewer==null) throw new BusinessException("请先登录",401);
        OrderVO order=orders.getDetail(id,viewer);
        String role=Objects.equals(order.getBuyerId(),viewer)?"BUYER":Objects.equals(order.getSellerId(),viewer)?"SELLER":null;
        if(role==null) throw new BusinessException("无权查看该订单",403);
        Integer status=order.getOrderStatus();boolean known=status!=null&&status>=0&&status<=5;
        String label=known?List.of("待付款","待发货","待收货","已完成","已取消","退款/售后").get(status):"状态待核对";
        List<String> notices=new ArrayList<>();
        if(!known)notices.add("当前状态无法识别，请与交易对方核对。");
        if(outOfSequence(order))notices.add("部分节点时间早于前序时间，以下按业务流程展示原记录，请核对。");
        boolean inconsistent=known&&switch(status) {
            case 0 -> any(order.getPayTime(),order.getShipTime(),order.getReceiveTime(),order.getCancelTime());
            case 1 -> any(order.getShipTime(),order.getReceiveTime(),order.getCancelTime());
            case 2 -> any(order.getReceiveTime(),order.getCancelTime());
            case 3,5 -> order.getCancelTime()!=null;
            default -> false;
        };
        if(inconsistent)notices.add("当前状态与已保存节点不完全一致，暂不判断下一位处理人。");
        List<Node> timeline=new ArrayList<>();
        timeline.add(new Node("created","提交订单",order.getCreateTime(),order.getCreateTime()==null?"UNRECORDED":"RECORDED","买家",order.getCreateTime()==null?"订单已存在，下单时间暂无记录。":"买家提交交易订单。"));
        boolean ended=Objects.equals(status,4);
        timeline.add(stage("paid","确认付款（模拟记录）",order.getPayTime(),"买家",
            Set.of(1,2,3,5).contains(status==null?-1:status)||any(order.getShipTime(),order.getReceiveTime()),ended||!known,
            "等待买家在订单中心确认模拟付款。"));
        timeline.add(stage("shipped","发货或交付标记",order.getShipTime(),"卖家",
            Set.of(2,3,5).contains(status==null?-1:status)||order.getReceiveTime()!=null,ended||!known,
            "由卖家根据实际交付安排标记发货。"));
        timeline.add(stage("completed","确认交易完成",order.getReceiveTime(),"买家",
            Objects.equals(status,3),ended||Objects.equals(status,5)||!known,
            "买家实际收到商品后，再确认收货完成交易。"));
        if(Objects.equals(status,5))timeline.add(new Node("after-sale","售后处理中",null,"UNRECORDED","买卖双方",
            "当前订单处于退款/售后状态，申请时间及完整过程未单独记录；平台仅记录协商结果。"));
        if(ended||order.getCancelTime()!=null)timeline.add(new Node("cancelled","订单取消或结束",order.getCancelTime(),order.getCancelTime()==null?"UNRECORDED":"RECORDED","具体操作人未单独记录",
            order.getCancelReason()==null||order.getCancelReason().isBlank()?"取消原因暂无记录。":"原因："+order.getCancelReason()));
        return new OrderOverviewVO(order,role,label,next(status,role,inconsistent),List.copyOf(timeline),List.copyOf(notices));
    }
    private Node stage(String key,String title,LocalDateTime time,String participant,boolean reached,boolean uncertain,String pending) {
        String state=time!=null?"RECORDED":reached?"UNRECORDED":uncertain?"UNKNOWN":"WAITING";
        String text=time!=null?"按订单中保存的节点时间展示。":reached?"订单已进入该阶段或后续阶段，该节点时间暂无记录。":uncertain?"没有该节点的时间记录，不能据此判断该步骤是否发生。":pending;
        return new Node(key,title,time,state,participant,text);
    }
    private NextStep next(Integer status,String viewer,boolean inconsistent) {
        if(inconsistent||status==null||status<0||status>5)return new NextStep("NONE","请核对订单记录","当前记录不足以判断下一步，请与对方核对。",false);
        String owner=switch(status){case 0,2->"BUYER";case 1,5->"SELLER";default->"NONE";};
        String title=switch(status){case 0->"等待买家确认付款";case 1->"等待卖家安排交付";case 2->"等待买家确认完成";case 3->"交易已完成";case 4->"订单已结束";default->"等待卖家处理售后";};
        String description=switch(status){
            case 0->"买家可在订单中心模拟确认付款，或在未付款时取消订单。";
            case 1->"卖家与买家确认交付安排后，在订单中心标记发货。";
            case 2->"买家实际收到商品后确认收货；如有问题，请先与卖家沟通。";
            case 3->"没有待处理的交易步骤，可在订单中心查看评价状态。";
            case 4->"没有待处理的交易步骤，可保留此页面作为交易记录。";
            default->"双方协商后，由卖家在订单中心处理售后记录；平台不执行真实退款。";
        };
        return new NextStep(owner,title,description,owner.equals(viewer));
    }
    private boolean any(LocalDateTime... times){return Arrays.stream(times).anyMatch(Objects::nonNull);}
    private boolean outOfSequence(OrderVO order){
        LocalDateTime previous=null;
        for(LocalDateTime time:Arrays.asList(order.getCreateTime(),order.getPayTime(),order.getShipTime(),order.getReceiveTime(),order.getCancelTime())){
            if(time!=null){if(previous!=null&&time.isBefore(previous))return true;previous=time;}
        }
        return false;
    }
}
