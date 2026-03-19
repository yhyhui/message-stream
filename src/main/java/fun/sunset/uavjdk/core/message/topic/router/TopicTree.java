package fun.sunset.uavjdk.core.message.topic.router;


import fun.sunset.uavjdk.core.message.gateway.IGateway;
import fun.sunset.uavjdk.core.message.topic.template.TopicTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT Topic 路由树
 * <p>
 * 使用 Trie 数据结构管理 Topic 模板与消费网关的映射关系
 * 支持 MQTT 通配符：
 * - "+" 单层通配符
 * - "#" 多层通配符
 * - "{var}" 变量占位符（当作 + 处理）
 */
public class TopicTree{

    private final TopicNode root = new TopicNode();

    /**
     * 用于反注册和动态重建时追踪所有网关
     */
    private final Set<IGateway> allListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 注册消费网关
     */
    public synchronized void register(IGateway gateway) {
        if (gateway == null || gateway.getTopic() == null) return;
        registerInternal(gateway.getTopic(), gateway);
        allListeners.add(gateway);
    }

    /**
     * 反注册消费网关，会自动重建树
     */
    public synchronized void unregister(IGateway gateway) {
        if (allListeners.remove(gateway)) {
            rebuild();
        }
    }

    /**
     * 重建整棵树（用于反注册后）
     */
    private void rebuild() {
        root.getChildren().clear();
        root.setPlusChild(null);
        root.setHashChild(null);
        root.getGateways().clear();

        for (IGateway gw : allListeners) {
            registerInternal(gw.getTopic(), gw);
        }
    }

    /**
     * 内部注册方法
     */
    private void registerInternal(TopicTemplate topic, IGateway gateway) {
        String[] parts = normalize(topic);
        TopicNode current = root;

        for (String part : parts) {
            // MQTT 规范：# 必须是最后一个层级
            if ("#".equals(part)) {
                if (current.getHashChild() == null) {
                    current.setHashChild(new TopicNode());
                }
                current.getHashChild().addGateway(gateway);
                return;
            }

            // 识别单层通配符：+ 或 {var} 形式的占位符
            boolean isVar = isVarToken(part);

            if ("+".equals(part) || isVar) {
                if (current.getPlusChild() == null) {
                    current.setPlusChild(new TopicNode());
                }
                current = current.getPlusChild();
            } else {
                current = current.getChildren()
                        .computeIfAbsent(part, k -> new TopicNode());
            }
        }
        current.addGateway(gateway);
    }

    /**
     * 判断是否为 {var} 形式的占位符
     */
    private boolean isVarToken(String part) {
        if (part == null) return false;
        int n = part.length();
        if (n < 3) return false;
        return part.charAt(0) == '{' && part.charAt(n - 1) == '}';
    }

    /**
     * 查询匹配给定物理 Topic 的所有消费网关
     * 返回的列表已按优先级排序（高优先级靠前）
     */
    public List<IGateway> match(String topic) {
        if (topic == null) return Collections.emptyList();

        Set<IGateway> result = new LinkedHashSet<>();
        matchRecursive(root, normalize(topic), 0, result);

        if (result.isEmpty()) return Collections.emptyList();

        // 按优先级排序：优先级大的排在前面
        return new ArrayList<>(result);
    }

    /**
     * 递归匹配算法
     */
    private void matchRecursive(TopicNode node, String[] parts, int index, Set<IGateway> result) {
        if (node == null) return;

        // 只要当前有 # 通配符，无论后面还有多少层，全部命中
        if (node.getHashChild() != null) {
            result.addAll(node.getHashChild().getGateways());
        }

        // 已到达末尾，收集当前节点的网关
        if (index == parts.length) {
            result.addAll(node.getGateways());
            return;
        }

        String part = parts[index];

        // 1. 精确匹配
        TopicNode exact = node.getChildren().get(part);
        if (exact != null) {
            matchRecursive(exact, parts, index + 1, result);
        }

        // 2. + 单层通配符匹配
        if (node.getPlusChild() != null) {
            matchRecursive(node.getPlusChild(), parts, index + 1, result);
        }
    }

    /**
     * 规范化 Topic（按 / 分割）
     * <p>
     * 遵循 MQTT 规范：
     * - "/a" 被解析为 ["", "a"]，表示以根开始
     * - "a/b" 被解析为 ["a", "b"]
     * - "#" 被解析为 ["#"]
     */
    private String[] normalize(TopicTemplate topic) {
        if (topic == null) {
            return new String[]{""};
        }
        String templateStr = topic.getTemplate();
        if (templateStr == null || templateStr.isEmpty()) {
            return new String[]{""};
        }
        // 使用 -1 保证末尾的空字符串不被丢弃（符合 MQTT 规范）
        return templateStr.split("/", -1);
    }
    private String[] normalize(String topic) {
        if (topic == null) {
            return new String[]{""};
        }
        if (topic.isEmpty()) {
            return new String[]{""};
        }
        return topic.split("/", -1);
    }
}
