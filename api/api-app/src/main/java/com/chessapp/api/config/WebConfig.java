package com.chessapp.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.chessapp.api.common.MdcFilter;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter filter) {
        FilterRegistrationBean<MdcFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(1);
        return reg;
    }
}
