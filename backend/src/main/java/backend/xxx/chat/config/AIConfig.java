package backend.xxx.chat.config;

import backend.xxx.chat.config.properties.AIProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AIProperties.class)
public class AIConfig {
}