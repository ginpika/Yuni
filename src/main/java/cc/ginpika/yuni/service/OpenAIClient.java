package cc.ginpika.yuni.service;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.core.YuniMessage;
import cc.ginpika.yuni.core.YuniSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OpenAIClient {
    @Resource
    YuniConfig apiConfig;

    ObjectMapper objectMapper = new ObjectMapper();

    private static final OkHttpClient client = new OkHttpClient().newBuilder()
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

    public String call(YuniSession session, String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url("https://coding.dashscope.aliyuncs.com/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiConfig.getApiKey())
                .addHeader("User-Agent", "openclaw")
                .post(RequestBody.create(requestBody.getBytes(StandardCharsets.UTF_8)))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String rawResponse = response.body().string();
                ObjectNode objectNode = (ObjectNode) objectMapper.readTree(rawResponse);
                String contentStr = objectNode.at("/choices/0/message/content").asText();
                log.info("回答：{}", contentStr);
                session.getMessages().add(YuniMessage.builder().role("assistant").content(contentStr).build());
                return rawResponse;
            } else {
                log.error("请求失败！状态码: {}, 错误信息: {}", response.code(), response.message());
                if (response.body() != null) {
                    log.error("详情: {}", response.body().string());
                }
                throw new IOException("API 请求失败: " + response.code());
            }
        }
    }
}
