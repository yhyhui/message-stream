package fun.sunset.uavjdk.core.message.topic.router;

import fun.sunset.uavjdk.core.message.gateway.IGateway;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQTT Topic Trie 节点
 * 
 * 存储 Topic 模板的树形结构，支持 MQTT 通配符（+ 和 #）
 */
@Getter
public class TopicNode {

    /**
     * 子节点映射：使用 ConcurrentHashMap 保证路径创建的线程安全
     */
    private final Map<String, TopicNode> children = new ConcurrentHashMap<>();

    /**
     * 单层通配符子节点 (+)
     */
    @Setter
    private TopicNode plusChild;

    /**
     * 多层通配符子节点 (#)
     */
    @Setter
    private TopicNode hashChild;

    /**
     * 当前节点的消费网关列表
     * 使用 CopyOnWriteArrayList 保证读多写少场景下的线程安全，
     * 避免在匹配消息（遍历列表）时因动态注册导致并发修改异常。
     */
    private final List<IGateway> gateways = new CopyOnWriteArrayList<>();

    /**
     * 添加消费网关并按优先级排序
     */
    public void addGateway(IGateway gateway) {
        if (gateway == null) return;

        synchronized (gateways) {
            if (!gateways.contains(gateway)) {
                gateways.add(gateway);
            }
        }
    }

    /**
     * 移除消费网关
     */
    public void removeGateway(IGateway gateway) {
        if (gateway == null) return;
        gateways.remove(gateway);
    }

    /**
     * 判断当前节点是否有消费网关
     */
    public boolean hasListeners() {
        return !gateways.isEmpty();
    }
}