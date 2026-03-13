package cc.ginpika.yuni.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "yuni")
@Data
public class YuniConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer contextWindow = 128000;
    private Integer maxTokens = 4096;
    private String workspace;
}
