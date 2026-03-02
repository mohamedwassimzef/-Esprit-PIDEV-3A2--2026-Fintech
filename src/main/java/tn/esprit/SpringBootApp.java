package tn.esprit;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringBootApp {
    // Entry point used by Main.java to bootstrap the Spring context alongside JavaFX
}

