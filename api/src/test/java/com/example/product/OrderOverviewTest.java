package com.example.product;

import com.example.exception.BusinessException;
import com.example.pojo.vo.*;
import com.example.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderOverviewTest {
    OrderService orders;OrderOverviewService service;OrderVO order;
    LocalDateTime start=LocalDateTime.of(2026,11,10,10,0);
    @BeforeEach void setup(){orders=mock(OrderService.class);service=new OrderOverviewService(orders);order=OrderVO.builder().id(1).orderNo("QA_ORDER").buyerId(2).sellerId(3).orderStatus(0).createTime(start).build();when(orders.getDetail(eq(1),any())).thenReturn(order);}
    @ParameterizedTest @CsvSource({"0,BUYER","1,SELLER","2,BUYER","3,NONE","4,NONE","5,SELLER"})
    void currentStatusIdentifiesNextResponsibleParty(int status,String expected){order.setOrderStatus(status);var result=service.read(1,2);assertEquals(expected,result.nextStep().responsibleRole());assertEquals(expected.equals("BUYER"),result.nextStep().yours());}
    @Test void sellerViewComesFromAuthenticatedOwner(){order.setOrderStatus(1);var result=service.read(1,3);assertEquals("SELLER",result.viewerRole());assertTrue(result.nextStep().yours());}
    @Test void frontendCannotChooseAnotherViewer(){assertEquals(403,assertThrows(BusinessException.class,()->service.read(1,4)).getCode());}
    @Test void missingIdentityOrInvalidOrderNumberRejectedBeforeReading(){assertThrows(BusinessException.class,()->service.read(0,2));assertThrows(BusinessException.class,()->service.read(-1,2));assertThrows(BusinessException.class,()->service.read(1,null));verifyNoInteractions(orders);}
    @Test void completeTimelineUsesOnlySavedTimes(){order.setOrderStatus(3);order.setPayTime(start.plusMinutes(1));order.setShipTime(start.plusMinutes(2));order.setReceiveTime(start.plusMinutes(3));var nodes=service.read(1,2).timeline();assertEquals(4,nodes.size());assertTrue(nodes.stream().allMatch(n->n.state().equals("RECORDED")));assertEquals(order.getReceiveTime(),nodes.get(3).time());}
    @Test void missingHistoricalCompletionTimeIsNotInventedFromUpdateTime(){order.setOrderStatus(3);order.setUpdateTime(start.plusDays(10));var node=service.read(1,2).timeline().get(3);assertNull(node.time());assertEquals("UNRECORDED",node.state());}
    @Test void cancelledOrderDoesNotPretendMissingStagesNeverHappened(){order.setOrderStatus(4);order.setCancelReason("双方协商取消");var result=service.read(1,2);assertEquals("UNKNOWN",result.timeline().get(1).state());var last=result.timeline().getLast();assertNull(last.time());assertTrue(last.description().contains("双方协商取消"));assertEquals("具体操作人未单独记录",last.participant());}
    @Test void afterSaleDoesNotInventApplicationTimeOrCompletion(){order.setOrderStatus(5);order.setUpdateTime(start.plusDays(10));var result=service.read(1,2);assertEquals("UNKNOWN",result.timeline().get(3).state());var last=result.timeline().getLast();assertEquals("after-sale",last.key());assertNull(last.time());}
    @Test void recordedCompletionRemainsVisibleWhenOrderLaterCancelled(){order.setOrderStatus(4);order.setReceiveTime(start.plusMinutes(3));order.setCancelTime(start.plusMinutes(4));var result=service.read(1,2);assertEquals("RECORDED",result.timeline().get(3).state());assertEquals(order.getCancelTime(),result.timeline().getLast().time());}
    @Test void impossibleCurrentStateDoesNotSuggestAFalseNextActor(){order.setPayTime(start.plusMinutes(1));var result=service.read(1,2);assertEquals("NONE",result.nextStep().responsibleRole());assertFalse(result.notices().isEmpty());}
    @Test void outOfOrderSavedTimesAreRetainedWithNotice(){order.setOrderStatus(2);order.setPayTime(start.minusDays(1));var result=service.read(1,2);assertEquals(start.minusDays(1),result.timeline().get(1).time());assertTrue(result.notices().stream().anyMatch(n->n.contains("早于前序")));}
    @Test void unknownStateDoesNotThrowOrAssumeWork(){order.setOrderStatus(99);assertEquals("NONE",service.read(1,2).nextStep().responsibleRole());order.setOrderStatus(null);assertEquals("状态待核对",service.read(1,2).statusLabel());}
    @Test void existingPermissionFailureIsPropagated(){when(orders.getDetail(1,2)).thenThrow(new BusinessException("无权查看",403));assertEquals(403,assertThrows(BusinessException.class,()->service.read(1,2)).getCode());}
}
