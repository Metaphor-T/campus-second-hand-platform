package com.example.service;

import com.example.campus.*;
import com.example.exception.BusinessException;
import com.example.mapper.CampusVerificationMapper;
import com.example.mapper.UserProfileMapper;
import com.example.pojo.vo.*;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CampusVerificationService {
    private final UserProfileService profiles;
    private final UserProfileMapper users;
    private final CampusVerificationMapper records;
    private final CampusVerificationProvider provider;

    public CampusVerificationVO mine() {
        var profile=profiles.mine();
        return view(records.latest(profile.id()),profile);
    }

    private UserProfileVO lockCurrent() {
        // 先锁后读：避免 MySQL REPEATABLE READ 在等待行锁前已建立旧资料快照。
        Integer id=UserContext.getUserId();
        if(id==null || !"USER".equals(UserContext.getRole())) throw new BusinessException("请使用普通用户身份登录",403);
        if(users.lockActiveUser(id)==null) throw new BusinessException("账号状态已变化",403);
        return profiles.mine();
    }
    private void requireProvider() {
        if(!provider.available()) throw new BusinessException("校园认证平台暂未接入，当前校园资料仍为本人填写",503);
        if(provider.key()==null || !provider.key().matches("[A-Za-z0-9_-]{1,64}"))
            throw new BusinessException("校园认证平台配置无效",503);
    }

    @Transactional(rollbackFor=Exception.class)
    public CampusVerificationVO apply() {
        var profile=lockCurrent();requireProvider();
        if(profile.school()==null || profile.school().isBlank() || profile.studentNumber()==null || profile.studentNumber().isBlank())
            throw new BusinessException("请先保存学校和学号，再申请校园身份认证",400);
        var previous=records.latest(profile.id());
        if(previous!=null && matches(previous,profile)) {
            String status=status(previous,profile);
            if("PENDING".equals(status) || "VERIFIED".equals(status)) return view(previous,profile);
        }
        var record=new CampusVerificationRecord();
        record.setBaseUserId(profile.id());record.setRequestId(UUID.randomUUID().toString());
        record.setSchool(profile.school());record.setStudentNumber(profile.studentNumber());
        record.setProvider(provider.key());record.setStatus("PENDING");
        records.insert(record);
        return view(records.latest(profile.id()),profile);
    }

    /** 前端只能请求后端查询，不能传入通过/驳回结果。调用发生在本地申请提交后。
     * 平台超时或异常保留 PENDING；后续以同一 requestId 幂等重试。 */
    @Transactional(rollbackFor=Exception.class)
    public CampusVerificationVO refresh() {
        var profile=lockCurrent();requireProvider();
        var record=records.latest(profile.id());
        if(record==null || !"PENDING".equals(status(record,profile))) return view(record,profile);
        if(!Objects.equals(record.getProvider(),provider.key())) throw new BusinessException("认证服务已变化，请联系管理员处理原申请",409);
        CampusVerificationProvider.Result result;
        try { result=provider.check(new CampusVerificationProvider.Request(record.getRequestId(),record.getSchool(),record.getStudentNumber())); }
        catch(Exception e) { throw new BusinessException("认证平台暂时无法响应，请稍后重试，原申请保留",502); }
        if(result==null || result.outcome()==null) throw new BusinessException("认证平台返回无效结果，请稍后重试",502);
        if(result.outcome()==CampusVerificationProvider.Outcome.PENDING) return view(record,profile);
        var now=LocalDateTime.now();
        if(result.evidenceReference()==null || result.evidenceReference().isBlank() || result.evidenceReference().length()>200
            || (result.outcome()==CampusVerificationProvider.Outcome.VERIFIED && (result.expiresAt()==null || !result.expiresAt().isAfter(now))))
            throw new BusinessException("认证平台结果缺少有效凭据或有效期",502);
        record.setStatus(result.outcome().name());record.setCheckedAt(now);
        record.setEvidenceReference(result.evidenceReference());
        record.setExpiresAt(result.outcome()==CampusVerificationProvider.Outcome.VERIFIED?result.expiresAt():null);
        if(records.complete(record)!=1) throw new BusinessException("认证申请已变化，请重新查看",409);
        return view(record,profile);
    }
    private boolean matches(CampusVerificationRecord record,UserProfileVO profile) {
        return Objects.equals(record.getSchool(),profile.school()) && Objects.equals(record.getStudentNumber(),profile.studentNumber());
    }
    private String status(CampusVerificationRecord record,UserProfileVO profile) {
        if(record==null) return "UNVERIFIED";
        if(!matches(record,profile)) return "INVALIDATED";
        if("VERIFIED".equals(record.getStatus()) && (record.getExpiresAt()==null || !record.getExpiresAt().isAfter(LocalDateTime.now()))) return "EXPIRED";
        return record.getStatus();
    }
    private CampusVerificationVO view(CampusVerificationRecord record,UserProfileVO profile) {
        String state=status(record,profile);
        String message=switch(state) {
            case "PENDING" -> "认证申请处理中，可刷新查询结果";
            case "VERIFIED" -> "已通过校园身份认证；不代表法律实名认证";
            case "REJECTED" -> "本次认证未通过，请核对学校和学号后重新申请";
            case "INVALIDATED" -> "学校或学号已修改，原认证失效，需要重新认证";
            case "EXPIRED" -> "校园身份认证已过期，需要重新认证";
            default -> "尚未申请校园身份认证";
        };
        if(!provider.available()) message="校园认证平台暂未接入。"+message;
        String number=record==null?null:record.getStudentNumber();
        String masked=number==null?null:(number.length()<=4?"****":"****"+number.substring(number.length()-4));
        return new CampusVerificationVO(provider.available(),state,message,record==null?null:record.getSchool(),masked,
            record==null?null:record.getRequestedAt(),record==null?null:record.getCheckedAt(),record==null?null:record.getExpiresAt());
    }
}
