package com.brainridge.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * <p>Running {@code main} boots the whole app: Spring starts an embedded web
 * server (Tomcat) on port 8080, scans the {@code com.brainridge.banking}
 * packages for components (controllers, services, repositories), and wires
 * them together automatically.
 *
 * <p>{@code @SpringBootApplication} bundles three annotations:
 * <ul>
 *   <li>{@code @Configuration} — this class can define beans.</li>
 *   <li>{@code @EnableAutoConfiguration} — Spring configures sensible defaults
 *       (JSON, validation, web server) based on the libraries on the classpath.</li>
 *   <li>{@code @ComponentScan} — finds and registers our annotated classes.</li>
 * </ul>
 */
@SpringBootApplication
public class BankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
