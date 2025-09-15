package com.company.amsbackend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<MdcEmployeeIdFilter> mdcEmployeeIdFilterRegistration(MdcEmployeeIdFilter filter) {
        FilterRegistrationBean<MdcEmployeeIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(1); // before Spring Security
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}