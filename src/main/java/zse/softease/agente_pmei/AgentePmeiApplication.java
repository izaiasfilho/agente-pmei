package zse.softease.agente_pmei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentePmeiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentePmeiApplication.class, args);
    }
}
