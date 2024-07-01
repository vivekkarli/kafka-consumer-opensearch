package io.consumer.kafka.consumers;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.gson.JsonParser;

import io.consumer.kafka.configurations.OpensearchClient;

@Component
public class OpensearchConsumer2 {

	private final Logger log = LoggerFactory.getLogger(OpensearchConsumer2.class);

	private final KafkaConsumer<String, String> kafkaConsumer;

	@Autowired
	public OpensearchConsumer2(KafkaConsumer<String, String> kafkaConsumer) {
		this.kafkaConsumer = kafkaConsumer;
	}

	@Scheduled(initialDelay = 2000)
	public void runOpensearchConsumer() {

		log.info("Thread: {} is vitual?: {}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());

		// step-1 define properties
		String topic = "wikimedia.recentchange";

		// create opensearch client
		RestHighLevelClient openSearchClient = OpensearchClient.createOpenSearchClient();
		try (openSearchClient; this.kafkaConsumer) {

			boolean doesIndexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"),
					RequestOptions.DEFAULT);

			if (!doesIndexExists) {
				CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
				openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
				log.info("wikimedia index has been created");
			} else {
				log.info("wikimedia index already exists");
			}

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

					String consumerRecordId = getIdFromJson(consumerRecord.value());

					IndexRequest indexRequest = new IndexRequest("wikimedia")
							.source(consumerRecord.value(), XContentType.JSON).id(consumerRecordId);

					IndexResponse indexResponse = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
					log.info("inserted record id: {} into opensearch, indexResponse.getId(): {}", consumerRecordId,
							indexResponse.getId());


				}

			}

		} catch (WakeupException we) {
			log.info("consumer started to shutdown");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			log.error("unexpected error occured in the consumer", e);
		} /*
			 * finally { kafkaConsumer.close();
			 * log.info("The consumer is now gracefully closed"); }
			 */

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

	}

	public static String getIdFromJson(String jsonString) {
		return JsonParser.parseString(jsonString)
				.getAsJsonObject()
				.get("meta").getAsJsonObject()
				.get("id")
				.getAsString();

	}

}
