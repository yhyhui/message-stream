package fun.sunset.uavjdk.core.message.gateway;

import fun.sunset.uavjdk.core.Lifecycle;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.topic.template.TopicTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public interface IGateway{

    void receive(Message<MessagePayload> message);

    TopicTemplate getTopic();

    MessageChannel getChannel();
}
