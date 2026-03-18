package fun.sunset.uavjdk.core.message.gateway;

import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.topic.template.TopicTemplate;
import jakarta.validation.constraints.NotNull;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public abstract class AbstractEndpoint implements IGateway {
    private final TopicTemplate topic;
    private final MessageChannel channel;

    public AbstractEndpoint(String topic) {
        this.topic = new TopicTemplate(topic);
        this.channel = new DirectChannel();
    }

    public AbstractEndpoint(String topic, @NotNull MessageChannel channel) {
        this.topic = new TopicTemplate(topic);
        this.channel = channel;
    }

    @Override
    public final void receive(Message<MessagePayload> message) {
        message = preProcess(message);
        process(message);
    }

    public Message<MessagePayload> preProcess(Message<MessagePayload> message) {
        return message;
    }

    public abstract void process(Message<MessagePayload> message);

    @Override
    public final TopicTemplate getTopic() {
        return topic;
    }

    @Override
    public final MessageChannel getChannel() {
        return channel;
    }
}
