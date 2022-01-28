package dev.vality.fraudbusters.mg.connector.converter;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.kafka.common.exception.TransportException;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentConverterImpl implements BinaryConverter<EventPayload> {

    ThreadLocal<TDeserializer> thriftDeserializerThreadLocal = ThreadLocal.withInitial(this::createDeserializer);

    @Override
    public EventPayload convert(byte[] bin, Class<EventPayload> clazz) {
        EventPayload event = new EventPayload();
        try {
            thriftDeserializerThreadLocal.get().deserialize(event, bin);
        } catch (TException e) {
            log.error("BinaryConverterImpl e: ", e);
        }
        return event;
    }

    private TDeserializer createDeserializer() {
        try {
            return new TDeserializer(new TBinaryProtocol.Factory());
        } catch (TTransportException ex) {
            throw new TransportException(ex);
        }
    }
}
