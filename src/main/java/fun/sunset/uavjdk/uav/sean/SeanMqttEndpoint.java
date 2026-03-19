package fun.sunset.uavjdk.uav.sean;

import fun.sunset.uavjdk.core.message.gateway.AbstractEndpoint;
import fun.sunset.uavjdk.core.message.model.MessagePayload;

import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class SeanMqttEndpoint extends AbstractEndpoint {
    public static final String TOPIC = "thing/product/{sn}/osd";

    public SeanMqttEndpoint(final Executor taskScheduler) {
        super(TOPIC, new ExecutorChannel(taskScheduler));
    }

    @Override
    public void process(Message<MessagePayload> message) {
        MessagePayload payload = message.getPayload();
    }
}
