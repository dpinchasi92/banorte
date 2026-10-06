package com.cbarkinozer.onlinebankingrestapi.app.gen.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class WebConfig {

    /** Spring Boot 3 dropped trailing-slash matching ("/customers/" no longer maps to "/customers"); keep the Boot 2 behavior. */
    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {

        FilterRegistrationBean<UrlHandlerFilter> registration =
                new FilterRegistrationBean<>(UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
