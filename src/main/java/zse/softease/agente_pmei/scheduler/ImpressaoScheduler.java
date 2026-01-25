package zse.softease.agente_pmei.scheduler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

@Component
public class ImpressaoScheduler {

    private final OrquestradorImpressaoService orquestradorImpressaoService;
    private final ConfiguracaoAgente  configuracaoAgente;

    public ImpressaoScheduler(OrquestradorImpressaoService orquestradorImpressaoService,
    		ConfiguracaoAgente configuracaoAgente) {
        this.orquestradorImpressaoService = orquestradorImpressaoService;
        this.configuracaoAgente = configuracaoAgente;
    }

    @Scheduled(fixedDelay = 1000)
    public void executar() {

    	  int intervalo = configuracaoAgente.getIntervaloMs();

    	    try {
    	        orquestradorImpressaoService.executarCiclo();
    	        Thread.sleep(intervalo);
    	    } catch (Exception e) {
    	        System.err.println("[AGENTE] erro: " + e.getMessage());
    	    }
    }
}
