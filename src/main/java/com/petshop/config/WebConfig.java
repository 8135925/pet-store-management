package com.petshop.config;

import com.petshop.interceptor.AdminInterceptor;
import com.petshop.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/vendor/**", "/images/**");

        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns(
                        "/system/**",
                        "/card-types/**",
                        "/services/save", "/services/*/toggle",
                        "/products/new", "/products/save", "/products/*/edit", "/products/*/toggle",
                        "/cards/*/void", "/cards/*/extend",
                        "/bills/*/cancel");
    }
}
