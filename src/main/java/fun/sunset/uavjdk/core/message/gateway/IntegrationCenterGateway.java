package fun.sunset.uavjdk.core.message.gateway;

import fun.sunset.uavjdk.core.message.gateway.builtin.UndefinedTopicGateway;
import fun.sunset.uavjdk.core.message.annotation.SsPublicGateway;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.topic.router.TopicTree;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.concurrent.Executor;

@IntegrationComponentScan
@MessagingGateway
@Getter
@SsPublicGateway
public class IntegrationCenterGateway extends AbstractRouter {
    public static final String TOPIC = "CenterGateway";

    /**
     * Topic 路由树：用于快速查询匹配的消费网关
     */
    private final TopicTree topicTree = new TopicTree();
    private final IntegrationFlowContext flowContext;

    @Resource
    private UndefinedTopicGateway undefinedTopicGateway;

    public IntegrationCenterGateway(List<IGateway> gateways, IntegrationFlowContext flowContext, Executor taskScheduler) {
        super(TOPIC, new ExecutorChannel(taskScheduler));
        this.flowContext = flowContext;
        for (IGateway gateway : gateways) {
            // 公共网关不允许传播订阅
            IntegrationFlow flow = IntegrationFlow.from(gateway.getChannel()).handle(gateway, "receive").get();
            this.flowContext.registration(flow).register();
            if (gateway.getClass().isAnnotationPresent(SsPublicGateway.class)) {
                continue;
            }
            topicTree.register(gateway);
        }
        register();
    }

    private void register() {
        IntegrationFlow flow = IntegrationFlow.from(this.getChannel()).handle(this, "receive").get();
        this.flowContext.registration(flow).register();
    }

    /**
     * 中心网关的消息发送方法
     * 逻辑：根据 Topic 匹配网关 -> 构造消息 -> 发送到对应网关的 Channel
     */
    @Override
    public List<IGateway> dispatch(Message<MessagePayload> message) {
        String topic = message.getPayload().getTopic();

        List<IGateway> matchedGateways = topicTree.match(topic);
        if (matchedGateways.isEmpty()) {
            return List.of(undefinedTopicGateway);
        }
        return matchedGateways;
    }
}
