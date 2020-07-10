package com.herod.leonides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@SpringBootApplication
@Configuration
public class LeonidesApplication {
	private static final Logger log = LoggerFactory.getLogger(LeonidesApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(LeonidesApplication.class, args);
	}

	@PostConstruct
	public void init()
	{
		log.info("Starting Leonides...");
	}


}
