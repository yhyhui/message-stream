package fun.sunset.uavjdk.core.message.connector;

import fun.sunset.uavjdk.core.message.gateway.IntegrationCenterGateway;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public abstract class AbstractInboundConnector {
    protected final MessageChannel channel = new DirectChannel();
    protected final IntegrationCenterGateway centerGateway;

    protected AbstractInboundConnector(IntegrationCenterGateway centerGateway) {
        this.centerGateway = centerGateway;
        IntegrationFlow flow = IntegrationFlow.from(this.channel).handle(this, "handle").get();
        this.centerGateway.getFlowContext().registration(flow).register();
    }

    /**
     * 统一的消息投递入口
     * 无论什么协议，最后都调用此方法发送到中心网关
     */
    protected final void sendToCenter(Message<MessagePayload> message) {
        centerGateway.getChannel().send(message);
    }

    public final void handle(Message<?> message) {
        MessagePayload payload = process(message);
        sendToCenter(payload.toMessage(message.getHeaders()));
    }

    protected abstract MessagePayload process(Message<?> message);
}