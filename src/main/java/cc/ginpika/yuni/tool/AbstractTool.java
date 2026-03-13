package cc.ginpika.yuni.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

@Getter
public abstract class AbstractTool implements Tool {
    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final String name;
    protected final String description;

    protected AbstractTool(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = buildProperties();
        if (properties != null && properties.size() > 0) {
            schema.set("properties", properties);
        }
        String[] required = getRequiredParameters();
        if (required != null && required.length > 0) {
            ObjectNode requiredNode = objectMapper.createObjectNode();
            for (String param : required) {
                requiredNode.put(param, true);
            }
            schema.set("required", requiredNode);
        }
        return schema;
    }

    protected ObjectNode buildProperties() {
        return objectMapper.createObjectNode();
    }

    protected String[] getRequiredParameters() {
        return new String[0];
    }

    protected ObjectNode createProperty(String type, String description) {
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", type);
        prop.put("description", description);
        return prop;
    }

    protected ObjectNode createProperty(String type, String description, String[] enumValues) {
        ObjectNode prop = createProperty(type, description);
        if (enumValues != null && enumValues.length > 0) {
            ObjectNode enumNode = objectMapper.createObjectNode();
            for (String value : enumValues) {
                enumNode.put(value, value);
            }
            prop.set("enum", enumNode);
        }
        return prop;
    }
}
