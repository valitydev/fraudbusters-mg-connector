package dev.vality.fraudbusters.mg.connector.deserializer;

import dev.vality.fraudbusters.mg.connector.utils.DeserializerUtils;
import dev.vality.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;

import java.util.Map;

@Slf4j
public class SinkEventDeserializer implements Deserializer<SinkEvent> {

    private ThreadLocal<TDeserializer> threadLocalDeserializer =
            ThreadLocal.withInitial(DeserializerUtils::createDeserializer);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public SinkEvent deserialize(String topic, byte[] data) {
        log.debug("Message, topic: {}, byteLength: {}", topic, data.length);
        SinkEvent machineEvent = new SinkEvent();
        try {
            threadLocalDeserializer.get().deserialize(machineEvent, data);
        } catch (Exception e) {
            log.error("Error when deserialize ruleTemplate data: {} ", data, e);
        }
        return machineEvent;
    }

    @Override
    public void close() {
        threadLocalDeserializer.remove();
    }

}
