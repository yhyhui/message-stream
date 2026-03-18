package fun.sunset.uavjdk.core.message.gateway.builtin;

import fun.sunset.uavjdk.core.message.annotation.SsPublicGateway;
import fun.sunset.uavjdk.core.message.model.MessagePayload;
import fun.sunset.uavjdk.core.message.gateway.AbstractEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@SsPublicGateway
public class UndefinedTopicGateway extends AbstractEndpoint {
    private final static Logger log = LoggerFactory.getLogger(UndefinedTopicGateway.class);
    private final static Executor executor = Executors.newVirtualThreadPerTaskExecutor();


    public final static String TOPIC = "UndefinedTopicGateway";

    public UndefinedTopicGateway() {
        super(TOPIC, new ExecutorChannel(executor));
    }

    private final List<MessageHandler> handlers = new CopyOnWriteArrayList<>();

    @Override
    public void process(Message<MessagePayload> message) {
        String topic = message.getPayload().getTopic();
        log.error("UndefinedTopicGateway receive message: {}", topic);
        // 每个handler在独立的虚拟线程中执行，异步非阻塞
        handlers.forEach(handler -> {
            executor.execute(() -> {
                try {
                    handler.handleMessage(message);
                    log.debug("Handler {} processed message for topic: {}", handler.getClass().getSimpleName(), topic);
                } catch (MessagingException e) {
                    log.error("Handler {} failed to process message for topic: {}",
                            handler.getClass().getSimpleName(), topic, e);
                } catch (Exception e) {
                    log.error("Unexpected error in handler {} for topic: {}",
                            handler.getClass().getSimpleName(), topic, e);
                }
            });
        });
    }

    public void subscribe(MessageHandler handler) {
        handlers.add(handler);
    }

    public void unsubscribe(MessageHandler handler) {
        handlers.remove(handler);
    }
}
