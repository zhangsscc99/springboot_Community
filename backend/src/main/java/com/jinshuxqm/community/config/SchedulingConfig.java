package com.jinshuxqm.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用定时任务的配置类
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 配置类，使@EnableScheduling注解生效
    // 定时任务的具体实现在ScheduledTasks类中
} 