package cc.ginpika.yuni.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YuniMessage {
    String role;
    String content;
    String rawResponse;

    public String toJsonString() {
        return "{ \"role\": %s, \"message\": %s}".formatted(role, content);
    }
}
