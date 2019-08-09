package com.ibm.developer.stormtracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/*
 * A quick and easy way to do database testing if you only depend a base database container. 
 * @author William.Korando@ibm.com
 *
 */
@SpringJUnitConfig
@ContextConfiguration(classes = { StormTrackerApplication.class }, initializers = ITStormRepo.Initializer.class)
@TestPropertySource("classpath:application.properties")
//Inherit the application.properties the application would really be using, override/add properties like JDBC URL below
//More demonstration purposes, ideally properties should be managed by environment and not packaged in application artifact
@TestMethodOrder(OrderAnnotation.class)
public class ITStormRepo {

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of("spring.datasource.url=jdbc:tc:storm_tracker_db:latest://arbitrary/arbitrary", //
					// JDBC url must start with "jdbc:tc" followed by type of database you are
					// connecting to
					"spring.datasource.username=arbitrary", //
					"spring.datasource.password=arbitrary", //
					//username/password can be arbitrary strings
					"spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver")//
					// Must use the ContainerDatabaseDriver which starts up the Docker container, is
					// eventually replaced with database appropriate driver
					.applyTo(applicationContext);
		}
	}

	@Autowired
	private StormRepo repo;

	@Test
	@Order(1)
	public void testReadFromStormsTable() {
		assertThat(repo.count()).isEqualTo(2);
	}

	@Test
	public void testWriteToStormsTable() {
		Storm savedStorm = repo.save(new Storm("03-17-2019", "03-20-2019", "South Atlantic", "Knoxville, Tennesee",
				"Tropical Depression", 3));
		assertThat(savedStorm.getId()).isEqualTo(12);
	}
}
