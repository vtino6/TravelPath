package com.travelpath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.travelpath")
@EnableJpaRepositories(basePackages = "com.travelpath.repository")
@EntityScan(basePackages = "com.travelpath.model")
public class TravelPathApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelPathApplication.class, args);
    }
}
