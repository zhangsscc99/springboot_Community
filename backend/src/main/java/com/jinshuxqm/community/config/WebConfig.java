package com.jinshuxqm.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 移除CORS配置，使用WebSecurityConfig中的配置
} 