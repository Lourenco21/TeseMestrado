package pt.lourenco.optimization.django.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "django.api")
public class DjangoApiProperties {

    private String baseUrl;
    private String problemDataPath;
}