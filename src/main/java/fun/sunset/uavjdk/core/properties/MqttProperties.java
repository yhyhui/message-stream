package fun.sunset.uavjdk.core.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@Getter
@Setter
@ConfigurationProperties(prefix = "ss.mqtt")
public class MqttProperties {
    private String ip;
    private String clientId;
    private String username;
    private String password;
    private MqttProperties.Port port;

    public MqttProperties() {
        this.ip = "127.0.0.1";
        this.clientId = UUID.randomUUID().toString();
        this.port = new MqttProperties.Port();
    }

    @Data
    public static class Port {
        private int tcp = 1883;
        private int tls = 8883;
        private int ws = 8083;
        private int wss = 8084;
        private int http = 8080;
    }
}
