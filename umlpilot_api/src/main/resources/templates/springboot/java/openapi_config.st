package $model.basePackage$.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI generatedOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Generated Backend API")
                        .description("API generada por UmlPilot a partir de un diagrama de clases")
                        .version("1.0.0"));
    }
}
