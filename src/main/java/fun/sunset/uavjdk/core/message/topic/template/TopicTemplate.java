package fun.sunset.uavjdk.core.message.topic.template;


import fun.sunset.uavjdk.core.message.model.MessageTraits;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 话题模板引擎：{var} 双向
 * - match：物理 topic -> variables
 * - render：variables -> 物理 topic
 */
public class TopicTemplate {

    private final String template;
    private final Pattern pattern;
    private final List<String> varNames;

    public TopicTemplate(String template) {
        this.template = Objects.requireNonNull(template);
        this.varNames = new ArrayList<>();

        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch == '{') {
                int j = template.indexOf('}', i);
                if (j < 0) throw new IllegalArgumentException("TopicTemplate 缺少右括号: " + template);
                String name = template.substring(i + 1, j).trim();
                if (name.isEmpty()) throw new IllegalArgumentException("TopicTemplate 变量名为空: " + template);
                varNames.add(name);
                regex.append("([^/]+)");
                i = j + 1;
            } else {
                // 转义正则特殊字符
                if ("\\.[]{}()+-^$|?".indexOf(ch) >= 0) regex.append("\\");
                regex.append(ch);
                i++;
            }
        }
        this.pattern = Pattern.compile("^" + regex + "$");
    }

    /**
     * 匹配物理 topic，返回变量值
     */
    public Optional<MessageTraits> match(String physicalTopic) {
        if (physicalTopic == null) return Optional.empty();
        Matcher m = pattern.matcher(physicalTopic);
        if (!m.matches()) return Optional.empty();

        MessageTraits vars = new MessageTraits();
        for (int k = 0; k < varNames.size(); k++) {
            vars.put(varNames.get(k), m.group(k + 1));
        }
        if (vars.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(vars);
    }

    public String render(Map<String, Object> vars) {
        if (vars == null) vars = Map.of();
        String out = template;
        for (String name : varNames) {
            Object v = vars.get(name);
            if (v == null) {
                throw new IllegalArgumentException("渲染 Topic 缺少变量: " + name + " template=" + template);
            }
            out = out.replace("{" + name + "}", (String) v);
        }
        return out;
    }

    public List<String> getVarNames() {
        return new ArrayList<>(varNames);
    }

    @SuppressWarnings("unused")
    public String getTemplate() {
        return template;
    }

    @SuppressWarnings("unused")
    public Pattern getPattern() {
        return pattern;
    }

}