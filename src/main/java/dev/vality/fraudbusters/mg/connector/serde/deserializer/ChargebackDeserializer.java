package dev.vality.fraudbusters.mg.connector.serde.deserializer;

import dev.vality.damsel.fraudbusters.Chargeback;
import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChargebackDeserializer extends AbstractThriftDeserializer<Chargeback> {

    @Override
    public Chargeback deserialize(String topic, byte[] data) {
        return deserialize(data, new Chargeback());
    }
}
