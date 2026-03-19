package fun.sunset.uavjdk.uav.mqtt;

import fun.sunset.uavjdk.core.message.connector.AbstractInboundConnector;
import fun.sunset.uavjdk.core.message.constant.ConstMessageHeader;
import fun.sunset.uavjdk.core.message.gateway.IntegrationCenterGateway;
import fun.sunset.uavjdk.core.message.model.EnumMessageSource;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.model.MessageTraits;
import fun.sunset.uavjdk.core.properties.MqttProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;


@Configuration
public class MqttInboundConnector extends AbstractInboundConnector {
    private final static Logger log = LoggerFactory.getLogger(MqttInboundConnector.class);

    private final MqttProperties mqttProperties;

    public MqttInboundConnector(IntegrationCenterGateway integrationCenterGateway, MqttProperties mqttProperties) {
        super(integrationCenterGateway);
        this.mqttProperties = mqttProperties;
    }

    @Override
    protected MessagePayload process(Message<?> message) {
        MessageHeaders headers = message.getHeaders();

        String topic = headers.get(ConstMessageHeader.MQTT_TOPIC, String.class);
        MessageTraits traits = new MessageTraits();
        traits.put(ConstMessageHeader.TAGET_TOPIC, topic);
        traits.put(ConstMessageHeader.SOURCE_GATEWAY_TYPE, EnumMessageSource.MQTT_GATEWAY.getCode());
        return MessagePayload.wrap(message.getPayload(), traits);
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        String brokerUrl = "tcp://" + mqttProperties.getIp() + ":" + mqttProperties.getPort().getTcp();
        String username = this.mqttProperties.getUsername();
        String password = this.mqttProperties.getPassword();
        options.setServerURIs(new String[]{brokerUrl});
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setKeepAliveInterval(180);
        options.setCleanSession(true);
        options.setAutomaticReconnect(true); // 建议开启自动重连
        factory.setConnectionOptions(options);

        log.info("MQTT 配置: url=> {}, username=> {}, pwd=> {}", brokerUrl, username, password);
        return factory;
    }


    @Bean
    public MessageProducer mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                this.mqttProperties.getClientId() + "_in",
                mqttClientFactory(),
                "#" // 拦截所有根主题流量
        );

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(super.channel);
        return adapter;
    }


}