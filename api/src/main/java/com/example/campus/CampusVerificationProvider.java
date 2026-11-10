package com.example.campus;

import java.time.LocalDateTime;

/** 仅由后端实现。学校适配器须按 requestId 幂等发起/查询、校验平台真实性并设置超时。
 * 不向用户开放提交认证结果的接口；不允许以自填资料或客户端状态判定通过。 */
public interface CampusVerificationProvider {
    String key();
    boolean available();
    Result check(Request request);
    record Request(String requestId, String school, String studentNumber) {}
    enum Outcome { PENDING, VERIFIED, REJECTED }
    record Result(Outcome outcome, String evidenceReference, LocalDateTime expiresAt) {}
}
