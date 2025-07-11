package dev.vality.fraudbusters.mg.connector.config;

import dev.vality.fraudbusters.mg.connector.serde.MachineEventSerde;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    public static final String WITHDRAWAL_SUFFIX = "-withdrawal";

    private final KafkaProperties kafkaProperties;
    private final ObjectProvider<SslBundles> sslBundles;

    @Bean
    public Properties mgInvoiceEventStreamProperties() {
        final Map<String, Object> props = kafkaProperties.buildStreamsProperties(sslBundles.getIfAvailable());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaProperties.getStreams().getApplicationId());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, kafkaProperties.getClientId());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, MachineEventSerde.class);
        addDefaultProperties(props);
        var properties = new Properties();
        properties.putAll(props);
        return properties;
    }

    @Bean
    public Properties mgWithdrawalEventStreamProperties() {
        final Map<String, Object> props = kafkaProperties.buildStreamsProperties(sslBundles.getIfAvailable());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG,
                kafkaProperties.getStreams().getApplicationId() + WITHDRAWAL_SUFFIX);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, kafkaProperties.getClientId() + WITHDRAWAL_SUFFIX);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, MachineEventSerde.class);
        addDefaultProperties(props);
        var properties = new Properties();
        properties.putAll(props);
        return properties;
    }

    private void addDefaultProperties(Map<String, Object> props) {
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndFailExceptionHandler.class);
    }

}
