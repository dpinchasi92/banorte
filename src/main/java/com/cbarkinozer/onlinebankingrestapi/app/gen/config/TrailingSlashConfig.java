package com.cbarkinozer.onlinebankingrestapi.app.gen.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Spring 6 no longer matches "/accounts/" to "/accounts". Keep accepting trailing slashes as before.
 */
@Configuration
public class TrailingSlashConfig {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {

        UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();

        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
