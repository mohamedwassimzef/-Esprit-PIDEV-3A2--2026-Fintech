package tn.esprit;

import controller.WebhookController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot application — provides embedded Tomcat + WebhookController.
 *
 * Started by Main.java in a daemon background thread before JavaFX UI opens.
 *
 * Key decisions:
 *  - @SpringBootApplication enables auto-configuration (provides the ServletWebServerFactory bean)
 *  - exclude DataSource/JPA auto-config because we use our own MyDB singleton
 *  - @ComponentScan is DISABLED (excludeFilters blocks everything) to prevent
 *    Spring from discovering other classes with main() methods or JavaFX controllers
 *  - WebhookController is registered explicitly as a @Bean
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = "tn.esprit",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*")
)
public class SpringBootApp {

    @Bean
    public WebhookController webhookController() {
        return new WebhookController();
    }
}
