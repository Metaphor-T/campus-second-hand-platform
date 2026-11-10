package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record OrderOverviewVO(OrderVO order,String viewerRole,String statusLabel,
    NextStep nextStep,List<Node> timeline,List<String> notices) {
    public record NextStep(String responsibleRole,String title,String description,boolean yours) {}
    public record Node(String key,String title,
        @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8") LocalDateTime time,
        String state,String participant,String description) {}
}
