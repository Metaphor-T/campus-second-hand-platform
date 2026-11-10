package com.example.campus;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CampusVerificationRecord {
    private Long id;
    private Integer baseUserId;
    private String requestId;
    private String school;
    private String studentNumber;
    private String provider;
    private String status;
    private String evidenceReference;
    private LocalDateTime requestedAt;
    private LocalDateTime checkedAt;
    private LocalDateTime expiresAt;
}
