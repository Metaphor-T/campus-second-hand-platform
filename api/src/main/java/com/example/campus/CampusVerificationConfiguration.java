package com.example.campus;

import com.example.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampusVerificationConfiguration {
    @Bean @ConditionalOnMissingBean(CampusVerificationProvider.class)
    public CampusVerificationProvider unavailableCampusProvider() {
        return new CampusVerificationProvider() {
            public String key() { return "NOT_CONNECTED"; }
            public boolean available() { return false; }
            public Result check(Request request) { throw new BusinessException("校园认证平台暂未接入",503); }
        };
    }
}
