package com.brainridge.banking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the interactive API documentation (Swagger UI).
 *
 * <p>The springdoc library reads this bean to build the page served at
 * {@code /swagger-ui/index.html}. It only affects documentation — it has no
 * impact on how the endpoints actually behave.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Provides the title, description, and version shown at the top of Swagger UI.
     * A {@code @Bean} method returns an object that Spring manages and injects
     * wherever it is needed (here, into springdoc).
     */
    @Bean
    public OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Transactions API")
                        .description("REST API for managing bank accounts and fund transfers")
                        .version("1.0.0"));
    }
}
