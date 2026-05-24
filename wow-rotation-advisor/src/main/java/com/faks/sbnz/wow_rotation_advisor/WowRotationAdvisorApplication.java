package com.faks.sbnz.wow_rotation_advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WowRotationAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(WowRotationAdvisorApplication.class, args);
	}

}
