package zse.softease.agente_pmei.scheduler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

@Component
public class ImpressaoScheduler {

    private final OrquestradorImpressaoService orquestradorImpressaoService;

    public ImpressaoScheduler(OrquestradorImpressaoService orquestradorImpressaoService) {
        this.orquestradorImpressaoService = orquestradorImpressaoService;
    }

    @Scheduled(fixedDelayString = "${agent.intervalo-ms:1000}")
    public void executar() {

        try {
        	orquestradorImpressaoService.executarCiclo();

        } catch (Exception e) {
            System.err.println("[AGENTE] erro: " + e.getMessage());
        }
    }
}
