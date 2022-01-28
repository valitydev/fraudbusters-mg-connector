package dev.vality.fraudbusters.mg.connector.serde.deserializer;

import dev.vality.kafka.common.exception.TransportException;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

import java.util.Map;

@Slf4j
public class MachineEventDeserializer implements Deserializer<MachineEvent> {

    ThreadLocal<TDeserializer> thriftDeserializerThreadLocal = ThreadLocal.withInitial(this::createDeserializer);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public MachineEvent deserialize(String topic, byte[] data) {
        log.debug("Message, topic: {}, byteLength: {}", topic, data.length);
        SinkEvent machineEvent = new SinkEvent();
        try {
            thriftDeserializerThreadLocal.get().deserialize(machineEvent, data);
        } catch (Exception e) {
            log.error("Error when deserialize ruleTemplate data: {} ", data, e);
        }
        return machineEvent.getEvent();
    }

    @Override
    public void close() {
        thriftDeserializerThreadLocal.remove();
    }

    private TDeserializer createDeserializer() {
        try {
            return new TDeserializer(new TBinaryProtocol.Factory());
        } catch (TTransportException ex) {
            throw new TransportException(ex);
        }
    }

}
