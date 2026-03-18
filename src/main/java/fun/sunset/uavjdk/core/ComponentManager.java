package fun.sunset.uavjdk.core;

import java.util.ArrayList;
import java.util.List;

public class ComponentManager {
    private final List<Lifecycle> components = new ArrayList<>();

    public void register(Lifecycle component) {
        components.add(component);
    }

    // 批量初始化
    public void initAll() {
        components.forEach(Lifecycle::init);
    }

    // 批量启动
    public void startAll() {
        components.forEach(Lifecycle::start);
    }

    // 优雅关闭
    public void destroyAll() {
        components.forEach(Lifecycle::destroy);
    }
}
