package com.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClassitApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassitApplication.class, args);
    }
}
