package fun.sunset.uavjdk.core.message.gateway;

import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.topic.template.TopicTemplate;
import jakarta.validation.constraints.NotNull;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

public abstract class AbstractRouter implements IGateway {
    private final TopicTemplate topic;
    private final MessageChannel channel;

    public AbstractRouter(String topic) {
        this.topic = new TopicTemplate(topic);
        this.channel = new DirectChannel();
    }

    public AbstractRouter(String topic, @NotNull MessageChannel channel) {
        this.topic = new TopicTemplate(topic);
        this.channel = channel;
    }

    @Override
    public final void receive(Message<MessagePayload> message) {
        List<IGateway> dispatch = dispatch(message);
        MessagePayload payload = message.getPayload();
        for (IGateway gateway : dispatch) {
            Message<MessagePayload> built = MessageBuilder.withPayload(payload.copy()).copyHeaders(message.getHeaders()).build();
            gateway.getChannel().send(built);
        }
    }

    public abstract List<IGateway> dispatch(Message<MessagePayload> message);

    @Override
    public final TopicTemplate getTopic() {
        return topic;
    }

    @Override
    public final MessageChannel getChannel() {
        return channel;
    }
}
