package dev.vality.fraudbusters.mg.connector.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stream")
public class StreamProperties {

    private boolean withdrawalEnabled;
    private boolean invoiceEnabled;

}
