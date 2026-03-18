package fun.sunset.uavjdk.core;

public interface Lifecycle {
    // 初始化（加载配置、预加载数据）
    void init();
    // 启动（开始处理请求）
    void start();
    // 销毁（释放资源）
    void destroy();
}