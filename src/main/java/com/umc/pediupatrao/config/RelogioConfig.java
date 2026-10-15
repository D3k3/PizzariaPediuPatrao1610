package com.umc.pediupatrao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelogioConfig {
    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
