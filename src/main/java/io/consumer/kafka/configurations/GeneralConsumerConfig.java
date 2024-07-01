package io.consumer.kafka.configurations;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneralConsumerConfig {

	@Bean
	public KafkaConsumer<String, String> getkafKafkaConsumer() {
		
		String group = "consumer-opensearch-group";
		Properties props = new Properties();
		props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
		props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		// cooperative re-balance
		props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
				CooperativeStickyAssignor.class.getName());

		// step-2 create consumer
		return new KafkaConsumer<>(props);
	}

}
