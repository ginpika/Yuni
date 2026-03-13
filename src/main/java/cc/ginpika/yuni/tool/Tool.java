package cc.ginpika.yuni.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Tool {

    String getName();

    String getDescription();

    ObjectNode getParametersSchema();

    String execute(JsonNode arguments);
}
