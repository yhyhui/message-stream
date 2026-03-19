package fun.sunset.uavjdk.uav.sean;

import fun.sunset.uavjdk.core.message.gateway.AbstractEndpoint;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class SeanWebHookEndpoint extends AbstractEndpoint {
    public static final String TOPIC = "sean/hook";

    @Resource
    private MongoTemplate mongoTemplate;

    public SeanWebHookEndpoint(final Executor taskScheduler) {
        super(TOPIC, new ExecutorChannel(taskScheduler));
    }

    @Override
    public void process(Message<MessagePayload> message) {
        MessagePayload payload = message.getPayload();
        HookContent hookContent = payload.getData(HookContent.class);

        mongoTemplate.insert(hookContent, "sean_web_hook");
    }


    @Setter
    @Getter
    public static class HookContent {
        private String uri;
        private String ipAddr;
        private LocalDateTime ts;

        private Map<String, String> headers;
        private Map<String, Object> params;
        private String body;
    }
}
