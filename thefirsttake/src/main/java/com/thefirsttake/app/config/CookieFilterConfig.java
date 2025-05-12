package com.thefirsttake.app.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieFilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> sameSiteCookieFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter((request, response, chain) -> {
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper((HttpServletResponse) response) {
                @Override
                public void addHeader(String name, String value) {
                    if ("Set-Cookie".equalsIgnoreCase(name)) {
                        value = value + "; SameSite=None";
                    }
                    super.addHeader(name, value);
                }
            };
            chain.doFilter(request, wrappedResponse);
        });
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
