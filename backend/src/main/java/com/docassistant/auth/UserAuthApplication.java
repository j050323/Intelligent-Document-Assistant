package com.docassistant.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.docassistant.auth", "com.docassistant.document"})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {"com.docassistant.auth.repository", "com.docassistant.document.repository"})
@EntityScan(basePackages = {"com.docassistant.auth.entity", "com.docassistant.document.entity"})
public class UserAuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserAuthApplication.class, args);
    }
}
