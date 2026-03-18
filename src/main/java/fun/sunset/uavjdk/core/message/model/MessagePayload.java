package fun.sunset.uavjdk.core.message.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fun.sunset.uavjdk.core.message.constant.ConstMessageHeader;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * MQTT/消息网关通用载荷封装类
 * 负责消息数据的序列化、反序列化、元数据（Header/Variable）管理，提供链式调用和深拷贝能力
 *
 */
@Getter
public class MessagePayload {
    /**
     * 变量映射表的Header键（用于存储业务自定义变量）
     */
    public static final String VARIABLE_KEY = "$$variable-map";

    private static final Logger log = LoggerFactory.getLogger(MessagePayload.class);

    /**
     * 全局ObjectMapper（线程安全，2.10+版本可安全复用）
     * 配置：支持JDK8时间类型、忽略未知字段、关闭多余序列化特性
     */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        // 静态初始化块：保证ObjectMapper只初始化一次，且配置完善
        OBJECT_MAPPER = new ObjectMapper();
        // 注册JDK8时间模块（解决LocalDateTime序列化/反序列化问题）
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        // 忽略JSON中存在但实体类没有的字段（避免反序列化失败）
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 关闭日期时间戳序列化（用ISO格式）
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 允许空值序列化（避免null字段报错）
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * 原始消息数据（二进制，适配MQTT等字节流传输场景）
     */
    private final byte[] data;

    /**
     * 消息特征（Header）：存储主题、QOS、业务标识等元数据
     */
    private final MessageTraits traits;

    /**
     * 链路追踪ID：全链路追踪用
     */
    private final String traceId;

    /**
     * 业务ID：标识单次业务请求
     */
    private final String businessId;

    /**
     * 消息创建时间
     */
    private final LocalDateTime ts;

    public static MessagePayload wrap(Object data, MessageTraits traits) {
        return switch (data) {
            case String s -> new MessagePayload(s, traits);
            case byte[] b -> new MessagePayload(b, traits);
            case MessagePayload p -> p;
            default -> throw new IllegalArgumentException("Unsupported payload type: " + data.getClass());
        };
    }


    /**
     * 核心构造器：封装任意类型数据为消息载荷
     *
     * @param data   业务数据（任意类型，会序列化为byte[]）
     * @param traits 消息特征（可为null，默认创建空Traits）
     */
    public MessagePayload(Object data, MessageTraits traits) {
        try {
            // 空值处理：data为null时赋值空数组（避免后续copy空指针）
            this.data = serializeData(data);
            // 空值处理：traits为null时创建空实例
            this.traits = Objects.requireNonNullElse(traits, new MessageTraits());
            // 链路ID：默认生成UUID（也可通过traits传入自定义值）
            this.traceId = getValueFromTraits("traceId", UUID.randomUUID().toString());
            // 业务ID：默认生成UUID（也可通过traits传入自定义值）
            this.businessId = getValueFromTraits("businessId", UUID.randomUUID().toString());
            // 创建时间：当前时间
            this.ts = LocalDateTime.now();
        } catch (JsonProcessingException e) {
            log.error("消息载荷序列化失败，原因：{}", e.getMessage(), e);
            throw new RuntimeException("数据序列化失败", e);
        }
    }

    /**
     * 私有构造器：用于深拷贝
     *
     * @param data       拷贝后的二进制数据
     * @param traits     拷贝后的消息特征
     * @param traceId    链路ID（复用原ID）
     * @param businessId 业务ID（复用原ID）
     */
    private MessagePayload(byte[] data, MessageTraits traits, String traceId, String businessId) {
        this.data = data;
        this.traits = traits;
        this.traceId = traceId;
        this.businessId = businessId;
        this.ts = LocalDateTime.now(); // 拷贝时生成新的时间戳（也可复用原ts，根据业务需求调整）
    }

    /**
     * 深拷贝：避免原对象修改影响拷贝对象
     *
     * @return 深拷贝后的MessagePayload
     */
    public MessagePayload copy() {
        // 数据拷贝：空数组处理（避免Arrays.copyOf空指针）
        byte[] copiedData = this.data == null ? new byte[0] : Arrays.copyOf(this.data, this.data.length);
        // Traits深拷贝：避免浅拷贝导致原对象traits修改影响拷贝对象
        MessageTraits copiedTraits = deepCopyTraits(this.traits);
        return new MessagePayload(copiedData, copiedTraits, this.traceId, this.businessId);
    }

    public Message<MessagePayload> toMessage(MessageHeaders headers) {
        return MessageBuilder.withPayload(this)
                .copyHeaders(headers)
                .build();
    }

    /**
     * 反序列化数据为指定类型
     *
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的业务对象
     */
    public <T> T getData(Class<T> clazz) {
        try {
            // 空值处理：空数组返回null
            if (this.data == null || this.data.length == 0) {
                return null;
            }
            return OBJECT_MAPPER.readValue(this.data, clazz);
        } catch (IOException e) {
            log.error("消息载荷反序列化失败，目标类型：{}，原因：{}", clazz.getName(), e.getMessage(), e);
            try {
                log.error("消息内容{}", OBJECT_MAPPER.writeValueAsString(this.data));
            } catch (JsonProcessingException ex) {
                log.error("消息内容反序列化失败", ex);
            }
            throw new RuntimeException("数据反序列化失败", e);
        }
    }

    /**
     * 反序列化复杂类型（如Map<String, Object>、List<T>）
     *
     * @param typeReference 泛型类型引用（如new TypeReference<Map<String, String>>() {}）
     * @param <T>           泛型类型
     * @return 反序列化后的业务对象
     */
    public <T> T getData(TypeReference<T> typeReference) {
        try {
            if (this.data == null || this.data.length == 0) {
                return null;
            }
            return OBJECT_MAPPER.readValue(this.data, typeReference);
        } catch (JsonProcessingException e) {
            log.error("消息载荷泛型反序列化失败，类型：{}，原因：{}", typeReference.getType(), e.getMessage(), e);
            throw new RuntimeException("泛型数据反序列化失败", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取自定义变量（带默认值）
     *
     * @param key          变量键
     * @param defaultValue 默认值
     * @param <T>          变量类型
     * @return 变量值（无值时返回默认值）
     */
    public <T> T getVariable(String key, T defaultValue) {
        Objects.requireNonNull(key, "变量键不能为空");
        Map<String, Object> variableMap = getHeader(VARIABLE_KEY, new HashMap<>());
        // 类型安全转换：避免ClassCastException
        return safeCast(variableMap.get(key), defaultValue);
    }

    /**
     * 获取自定义变量（无默认值）
     *
     * @param key 变量键
     * @param <T> 变量类型
     * @return 变量值（无值时返回null）
     */
    public <T> T getVariable(String key) {
        return getVariable(key, null);
    }

    /**
     * 设置自定义变量（链式调用）
     *
     * @param key   变量键（不能为空）
     * @param value 变量值（可为null）
     * @return 当前对象（链式调用）
     */
    public MessagePayload putVariable(String key, Object value) {
        Objects.requireNonNull(key, "变量键不能为空");
        Map<String, Object> variableMap = getHeader(VARIABLE_KEY, new HashMap<>());
        variableMap.put(key, value);
        this.traits.put(VARIABLE_KEY, variableMap);
        return this;
    }

    /**
     * 获取Header（带默认值）
     *
     * @param key          Header键
     * @param defaultValue 默认值
     * @param <T>          Header类型
     * @return Header值（无值时返回默认值）
     */
    public <T> T getHeader(String key, T defaultValue) {
        Objects.requireNonNull(key, "Header键不能为空");
        Object value = this.traits.get(key);
        return safeCast(value, defaultValue);
    }

    /**
     * 获取Header（无默认值）
     *
     * @param key Header键
     * @param <T> Header类型
     * @return Header值（无值时返回null）
     */
    public <T> T getHeader(String key) {
        return getHeader(key, null);
    }

    /**
     * 设置Header（链式调用）
     *
     * @param key   Header键（不能为空）
     * @param value Header值（可为null）
     * @return 当前对象（链式调用）
     */
    public MessagePayload putHeader(String key, Object value) {
        Objects.requireNonNull(key, "Header键不能为空");
        Objects.requireNonNull(this.traits, "消息特征Traits不能为空");
        this.traits.put(key, value);
        return this;
    }

    /**
     * 序列化任意数据为byte[]
     *
     * @param data 待序列化数据
     * @return 二进制数组（null/空值返回空数组）
     * @throws JsonProcessingException 序列化异常
     */
    private byte[] serializeData(Object data) throws JsonProcessingException {
        if (data == null) {
            return new byte[0];
        }
        if (data instanceof byte[]) {
            return (byte[]) data;
        }
        return OBJECT_MAPPER.writeValueAsBytes(data);
    }

    /**
     * 从Traits中获取值（无值时返回默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 值
     */
    private <T> T getValueFromTraits(String key, T defaultValue) {
        Object value = this.traits.get(key);
        return value == null ? defaultValue : safeCast(value, defaultValue);
    }

    /**
     * 安全类型转换（避免ClassCastException）
     *
     * @param value        待转换值
     * @param defaultValue 默认值（用于获取目标类型）
     * @param <T>          目标类型
     * @return 转换后的值（类型不匹配返回默认值）
     */
    @SuppressWarnings("unchecked")
    private <T> T safeCast(Object value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("类型转换失败，值：{}，目标类型：{}，返回默认值：{}",
                    value, defaultValue.getClass().getName(), defaultValue);
            return defaultValue;
        }
    }

    /**
     * 深拷贝MessageTraits（避免浅拷贝）
     *
     * @param traits 原Traits
     * @return 深拷贝后的Traits
     */
    private MessageTraits deepCopyTraits(MessageTraits traits) {
        if (traits == null) {
            return new MessageTraits();
        }
        try {
            // 利用Jackson进行深拷贝（比手动拷贝更可靠）
            return OBJECT_MAPPER.readValue(
                    OBJECT_MAPPER.writeValueAsBytes(traits),
                    MessageTraits.class
            );
        } catch (IOException e) {
            log.error("MessageTraits深拷贝失败，原因：{}", e.getMessage(), e);
            throw new RuntimeException("Traits拷贝失败", e);
        }
    }

    /**
     * 快速创建空载荷
     *
     * @return 空MessagePayload
     */
    public static MessagePayload empty() {
        return new MessagePayload(null, new MessageTraits());
    }

    /**
     * 获取原始数据的字符串形式（UTF-8编码）
     *
     * @return 字符串数据
     */
    @JsonIgnore
    public String getDataAsString() {
        if (this.data == null || this.data.length == 0) {
            return "";
        }
        return new String(this.data);
    }


    public String getTopic() {
        return this.traits.get(ConstMessageHeader.TAGET_TOPIC);
    }

    public EnumMessageSource getMessageSource() {
        return EnumMessageSource.find(this.traits.get(ConstMessageHeader.SOURCE_GATEWAY_TYPE));
    }

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("MessagePayload序列化失败，原因：{}", e.getMessage(), e);
            return "MessagePayload序列化失败";
        }
    }
}
