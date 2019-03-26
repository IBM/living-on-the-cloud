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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/*
 * An alternate example of using Testcontainers for database testing. 
 * This example directly starts up a Testcontainer and the pulls the 
 * relevant info on connecting to the container from it.
 * 
 * @author William.Korando@ibm.com
 *
 */
@Testcontainers
@SpringJUnitConfig
@ContextConfiguration(classes = {
		StormTrackerApplication.class }, initializers = ITStormRepoAlternate.Initializer.class)
@TestPropertySource("classpath:application.properties")
@TestMethodOrder(OrderAnnotation.class)
public class ITStormRepoAlternate {

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of("spring.datasource.url=" + container.getJdbcUrl(), //Pull from the container the JDBC connection URL
					"spring.datasource.username=" + container.getUsername(), //Pull from the container the username to connect to the containerized database (default is "test")
					"spring.datasource.password=" + container.getPassword(), //Pull from the container the password to connect to the containerized database (default is "test")
					"spring.jpa.properties.hibernate.hbm2ddl.import_files=data.sql", //
					"spring.jpa.hibernate.ddl-auto=create-drop")
					.applyTo(applicationContext);
		}
	}

	@Container
	private static PostgreSQLContainer container = new PostgreSQLContainer("postgres:11.2");//Can be an arbitrary image name and tag

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
