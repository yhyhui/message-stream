package fun.sunset.uavjdk.core.message.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageTraits extends HashMap<String, Object> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MessageTraits.class);

    public MessageTraits() {

    }

    public MessageTraits(MessageTraits traits) {
        super(traits);
    }

    public MessageTraits(Map<String, ?> traits) {
        super(traits);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) super.get(key);
        } catch (Exception e) {
            logger.error("MessageTraits get error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public <T> T get(String key, T defaultValue) {
        T value = get(key);
        return value == null ? defaultValue : value;
    }

    public MessageTraits put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
