package com.w3villa.mini_project_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiniProjectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniProjectBackendApplication.class, args);
	}

}
