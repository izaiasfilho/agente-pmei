package zse.softease.agente_pmei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
@SpringBootApplication
@EnableScheduling
public class AgentePmeiApplication {

    public static void main(String[] args) {

        ConfiguracaoAgente config = ConfiguracaoAgente.carregar();

        SpringApplication app = new SpringApplication(AgentePmeiApplication.class);
        app.addInitializers(ctx ->
                ctx.getBeanFactory()
                   .registerSingleton("configuracaoAgente", config)
        );

        app.run(args);
    }
}
