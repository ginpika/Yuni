package cc.ginpika.yuni.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String rawResponse;
    private boolean needPermission;
    private String requestId;
    private String command;
    private boolean success;
    private String error;
}
