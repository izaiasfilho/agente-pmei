package zse.softease.agente_pmei.scheduler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

@Component
public class ImpressaoScheduler {

    private final OrquestradorImpressaoService orquestradorImpressaoService;
    private final ConfiguracaoAgente  configuracaoAgente;
    private final AgentStateService agentStateService;
    private final LogService logService;

    public ImpressaoScheduler(OrquestradorImpressaoService orquestradorImpressaoService,
    		ConfiguracaoAgente configuracaoAgente,
    		AgentStateService agentStateService,
    		LogService logService) {
        this.orquestradorImpressaoService = orquestradorImpressaoService;
        this.configuracaoAgente = configuracaoAgente;
        this.agentStateService = agentStateService;
        this.logService = logService;
    }

    @Scheduled(fixedDelay = 1000)
    public void executar() {

        try {
           agentStateService.marcarRodando("Executando ciclo");
           agentStateService.atualizarUltimaExecucao();

            orquestradorImpressaoService.executarCiclo();

           logService.info("Ciclo executado com sucesso");

            Thread.sleep(configuracaoAgente.getIntervaloMs());

        } catch (Exception e) {

            agentStateService.marcarErro(e.getMessage());
            logService.erro("Erro no ciclo do agente", e.getMessage());
        }
    }

}
