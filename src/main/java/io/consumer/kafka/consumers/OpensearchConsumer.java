package io.consumer.kafka.consumers;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpensearchConsumer {

	private final Logger log = LoggerFactory.getLogger(OpensearchConsumer.class);

	//@Scheduled(initialDelay = 2000)
	public void runOpensearchConsumer() {

		log.info("Thread: {} is vitual?: {}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());

		// step-1 define properties
		String topic = "wikimedia.recentchange";
		String group = "consumer-opensearch-group";
		Properties props = new Properties();
		props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
		props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		
		//cooperative re-balance
		props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());

		// step-2 create consumer
		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);

		// get reference of current thread
		final Thread mainThread = Thread.currentThread();

		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				log.info("shutdown detected. exiting by calling consumer.wakeup()");

				kafkaConsumer.wakeup();

				try {
					mainThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});

		try {

			// step-3 subscribe to topic
			kafkaConsumer.subscribe(List.of(topic));

			// step-4 poll for data
			while (true) {

				log.info("polling...");

				ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

				for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
					log.info("key: {}", consumerRecord.key());
					log.info("value: {}", consumerRecord.value());
					log.info("topic: {}", consumerRecord.topic());
					log.info("partition: {}", consumerRecord.partition());
					log.info("offset: {}", consumerRecord.offset());
				}

			}

		} catch (WakeupException we) {
			log.info("consumer started to shutdown");
		} catch (Exception e) {
			log.error("unexpected error occured in the consumer", e);
		} finally {
			kafkaConsumer.close();
			log.info("The consumer is now gracefully closed");
		}

	}

}
