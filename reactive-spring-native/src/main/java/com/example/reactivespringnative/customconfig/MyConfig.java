package com.example.reactivespringnative.customconfig;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MyConfig {

    @Bean
    ApplicationRunner ar()
    {
        
        return args -> System.out.println("Cutsom Config");
    }
    
}
