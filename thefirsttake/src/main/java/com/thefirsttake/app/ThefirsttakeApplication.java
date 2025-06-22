package com.thefirsttake.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ThefirsttakeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThefirsttakeApplication.class, args);
	}

}
