package zse.softease.agente_pmei.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

@Component
public class ImpressaoScheduler {

    private static final int POLLING_PICO_MS = 1000;
    private static final int POLLING_NORMAL_MS = 3000;
    private static final int POLLING_BAIXO_PICO_MS = 10000;
    private static final int POLLING_MAXIMO_MS = 15000;

    private final OrquestradorImpressaoService orquestradorImpressaoService;
    private final AgentStateService agentStateService;
    private final LogService logService;

    private int ciclosSemJob = 0;
    private int proximoPollingMs = POLLING_NORMAL_MS;

    public ImpressaoScheduler(
            OrquestradorImpressaoService orquestradorImpressaoService,
            AgentStateService agentStateService,
            LogService logService) {
        this.orquestradorImpressaoService = orquestradorImpressaoService;
        this.agentStateService = agentStateService;
        this.logService = logService;
    }

    @Scheduled(fixedDelay = 100)
    public void executar() {
        try {
            agentStateService.marcarRodando("Executando ciclo");
            agentStateService.atualizarUltimaExecucao();

            Integer pollingHint = orquestradorImpressaoService.executarCiclo();

            if (pollingHint != null) {
                // Job was processed
                ciclosSemJob = 0;
                if (pollingHint > 0) {
                    proximoPollingMs = Math.max(POLLING_PICO_MS, Math.min(POLLING_MAXIMO_MS, pollingHint));
                } else {
                    proximoPollingMs = POLLING_PICO_MS;
                }
            } else {
                // No job found — adaptive escalation
                ciclosSemJob++;
                if (ciclosSemJob < 3) {
                    proximoPollingMs = POLLING_NORMAL_MS;
                } else if (ciclosSemJob < 6) {
                    proximoPollingMs = POLLING_BAIXO_PICO_MS;
                } else {
                    proximoPollingMs = POLLING_MAXIMO_MS;
                }
            }

            LocalDateTime proximaConsulta = LocalDateTime.now()
                    .plusNanos((long) proximoPollingMs * 1_000_000);
            agentStateService.atualizarPollingInfo(ciclosSemJob, proximoPollingMs, proximaConsulta);

            Thread.sleep(proximoPollingMs);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            ciclosSemJob++;
            proximoPollingMs = POLLING_MAXIMO_MS;
            agentStateService.marcarErro(e.getMessage());
            logService.erro("Erro no ciclo do agente", e.getMessage());
            try {
                LocalDateTime proximaConsulta = LocalDateTime.now()
                        .plusNanos((long) proximoPollingMs * 1_000_000);
                agentStateService.atualizarPollingInfo(ciclosSemJob, proximoPollingMs, proximaConsulta);
                Thread.sleep(proximoPollingMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
