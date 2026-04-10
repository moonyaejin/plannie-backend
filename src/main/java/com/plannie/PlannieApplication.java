package com.plannie;

import com.plannie.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class PlannieApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlannieApplication.class, args);
    }
}
