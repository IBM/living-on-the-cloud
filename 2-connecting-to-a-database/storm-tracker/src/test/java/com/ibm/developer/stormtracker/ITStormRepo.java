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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = { StormTrackerApplication.class }, initializers = ITStormRepo.Initializer.class)
@TestMethodOrder(OrderAnnotation.class)
public class ITStormRepo {

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues
					.of("spring.datasource.url=jdbc:tc:postgresql:11.2://localhost/test",// 
							"spring.datasource.username=admin", //
							"spring.datasource.password=admin", //
							"spring.jpa.properties.hibernate.hbm2ddl.import_files=data.sql", //
							"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL95Dialect", //
							"spring.jpa.hibernate.ddl-auto=create-drop", //
							"spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver")
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
