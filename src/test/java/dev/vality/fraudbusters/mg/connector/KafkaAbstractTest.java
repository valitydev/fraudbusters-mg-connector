package dev.vality.fraudbusters.mg.connector;

import dev.vality.fraudbusters.mg.connector.extension.KafkaExtension;
import dev.vality.fraudbusters.mg.connector.serde.deserializer.MachineEventDeserializer;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ExtendWith({SpringExtension.class, KafkaExtension.class})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextConfiguration(initializers = KafkaAbstractTest.Initializer.class)
public abstract class KafkaAbstractTest {

    public static final String MG_EVENT = "mg-event";
    public static final String MG_WITHDRAWAL = "mg-withdrawal";
    public static final String PAYMENT = "payment";
    public static final String REFUND = "refund";
    public static final String CHARGEBACK = "chargeback";
    public static final String WITHDRAWAL = "withdrawal";

    public static <T> Consumer<String, T> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaExtension.KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MachineEventDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    public static <T> Consumer<String, T> createPaymentConsumer(Class clazz) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaExtension.KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, clazz);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    public static <T> Producer<String, T> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaExtension.KAFKA.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "CLIENT");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        return new KafkaProducer<>(props);
    }

    void produceMessageToEventSink(String topic, SinkEvent sinkEvent) {
        try (Producer<String, SinkEvent> producer = createProducer()) {
            ProducerRecord<String, SinkEvent> producerRecord =
                    new ProducerRecord<>(topic, sinkEvent.getEvent().getSourceId(), sinkEvent);
            producer.send(producerRecord).get();
            log.info("produceMessageToEventSink() sinkEvent: {}", sinkEvent);
        } catch (Exception e) {
            log.error("Error when produceMessageToEventSink e:", e);
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues
                    .of("spring.kafka.bootstrap-servers=" + KafkaExtension.KAFKA.getBootstrapServers())
                    .applyTo(configurableApplicationContext.getEnvironment());
            initTopic(MG_EVENT);
            initTopic(PAYMENT);
            initTopic(REFUND);
            initTopic(CHARGEBACK);
            initTopic(MG_WITHDRAWAL);
            initTopic(WITHDRAWAL);
        }

        private <T> Consumer<String, T> initTopic(String topicName) {
            Consumer<String, T> consumer = createConsumer();
            try {
                consumer.subscribe(Collections.singletonList(topicName));
                consumer.poll(Duration.ofSeconds(1));
            } catch (Exception e) {
                log.error("KafkaAbstractTest initialize e: ", e);
            }
            consumer.close();
            return consumer;
        }
    }
}
