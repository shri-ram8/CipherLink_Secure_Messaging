package com.cipherlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CipherLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(CipherLinkApplication.class, args);
    }
}
