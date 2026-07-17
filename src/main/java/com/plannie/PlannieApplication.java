package com.plannie;

import com.plannie.config.AdminProperties;
import com.plannie.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, AdminProperties.class})
@EnableScheduling
public class PlannieApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlannieApplication.class, args);
    }
}
