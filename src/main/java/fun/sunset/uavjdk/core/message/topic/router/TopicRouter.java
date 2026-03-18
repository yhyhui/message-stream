package fun.sunset.uavjdk.core.message.topic.router;

import fun.sunset.uavjdk.core.message.gateway.IGateway;

import java.util.List;

public interface TopicRouter {
    List<IGateway> match(String topic);

    void register(IGateway gateway);
}
