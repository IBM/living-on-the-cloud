package com.ibm.developer.stormtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StormTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StormTrackerApplication.class, args);
		System.setProperty("https.protocols", "SSLv3");
	}

}
