package com.example.pojo.vo;

import java.time.LocalDateTime;

public record CampusVerificationVO(boolean available, String status, String message,
    String school, String maskedStudentNumber, LocalDateTime requestedAt,
    LocalDateTime checkedAt, LocalDateTime expiresAt) {}
