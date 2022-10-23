package com.example.tidbnmbackend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class TidbNmBackendApplication {
public static String IP;
    public static void main(String[] args) {
        IP =args[0];
        SpringApplication.run(TidbNmBackendApplication.class, args);
    }

}
