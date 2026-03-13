package cc.ginpika.yuni.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "yuni")
@Data
public class YuniConfig {
    String apiKey;
    String baseUrl;
    String model;
}
