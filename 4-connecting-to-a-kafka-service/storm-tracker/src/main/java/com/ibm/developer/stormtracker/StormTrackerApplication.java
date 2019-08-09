package com.ibm.developer.stormtracker;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@SpringBootApplication
public class StormTrackerApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(StormTrackerApplication.class, args);
	}

//	@Bean
//	public ProducerFactory<String, String> producerFactory() {
//		Map<String, Object> configProps = new HashMap<String, Object>();
//		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//		configProps.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
//		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//		configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
//		configProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
//		configProps.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
//		configProps.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
//		configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
//
//		return new DefaultKafkaProducerFactory<String, String>(configProps);
//	}
//
//	@Bean
//	public ConsumerFactory<String, String> consumerFactory() {
//		Map<String, Object> configProps = new HashMap<String, Object>();
//		configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//		configProps.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
//		configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//		configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
//		configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
//		configProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
//		configProps.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
//		configProps.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
//		configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
//
//		return new DefaultKafkaConsumerFactory<String, String>(configProps);
//	}
	@Bean
	public KafkaTemplate<String, String > kafkaTemplate(ProducerFactory<String, String> producerFactory) {
	    return new KafkaTemplate<String, String>(producerFactory);
	}
	
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String,String> consumerFactory) {
	    ConcurrentKafkaListenerContainerFactory<String, String> factory
	            = new ConcurrentKafkaListenerContainerFactory<String, String>();
	    factory.setConsumerFactory(consumerFactory);
	    return factory;
	}
}
