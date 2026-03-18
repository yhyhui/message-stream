package fun.sunset.uavjdk.core.message.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表示不同状态的枚举类，每个枚举值对应一个描述信息。
 * 支持通过枚举的序号（ordinal）和名称（name）查找对应的枚举值。
 */
public enum EnumMessageSource {
    UNKNOWN("", "无"),
    CENTER_GATEWAY("CENTER_GATEWAY", ""),
    MQTT_GATEWAY("MQTT", "MQTT");

    // 定义两个静态映射：一个根据 ordinal 查找，另一个根据 name 查找
    private static final Map<Integer, EnumMessageSource> ENUM_MAP_BY_ORDINAL;
    private static final Map<String, EnumMessageSource> ENUM_MAP_BY_NAME;
    private static final Map<String, EnumMessageSource> ENUM_MAP_BY_CODE;

    @JsonValue
    private final String code;
    private final String description;

    // 枚举构造方法
    EnumMessageSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    // 静态代码块，用于初始化静态映射
    static {
        ENUM_MAP_BY_ORDINAL = Arrays.stream(EnumMessageSource.values())
                .collect(Collectors.toMap(EnumMessageSource::ordinal, v -> v));
        ENUM_MAP_BY_NAME = Arrays.stream(EnumMessageSource.values())
                .collect(Collectors.toMap(EnumMessageSource::name, v -> v));
        ENUM_MAP_BY_CODE = Arrays.stream(EnumMessageSource.values())
                .collect(Collectors.toMap(EnumMessageSource::getCode, v -> v));
    }

    /**
     * 根据枚举的序号（ordinal）获取对应的枚举值。
     *
     * @param ordinal 枚举的序号
     * @return 对应的枚举值，如果序号无效则返回 NONE
     */
    public static EnumMessageSource get(int ordinal) {
        // 如果传入的 ordinal 无效，返回 NONE
        return ENUM_MAP_BY_ORDINAL.getOrDefault(ordinal, UNKNOWN);
    }

    /**
     * 根据枚举的名称（name）获取对应的枚举值。
     *
     * @param name 枚举的名称
     * @return 对应的枚举值，如果名称无效则返回 NONE
     */
    public static EnumMessageSource get(String name) {
        // 如果传入的名称无效，返回 NONE
        return ENUM_MAP_BY_NAME.getOrDefault(name, UNKNOWN);
    }

    public static EnumMessageSource find(String code) {
        // 如果传入的名称无效，返回 NONE
        return ENUM_MAP_BY_CODE.getOrDefault(code, UNKNOWN);
    }

    /**
     * 获取枚举值的描述信息。
     *
     * @return 枚举值的描述信息
     */
    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}