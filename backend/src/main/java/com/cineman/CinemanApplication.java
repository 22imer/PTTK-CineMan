package com.cineman;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class CinemanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinemanApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Đồng hồ UTC dùng chung cho phiên JWT; test biên thời gian thay bằng {@code @Primary} Clock. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
