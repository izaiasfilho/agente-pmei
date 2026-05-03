package zse.softease.agente_pmei.service;

import java.time.LocalDateTime;

import zse.softease.agente_pmei.dto.AgentStateDTO;

public interface AgentStateService {

    void marcarRodando(String mensagem);

    void marcarErro(String mensagem);

    void atualizarUltimaExecucao();

    void atualizarPollingInfo(int ciclosSemJob, int proximoPollingMs, LocalDateTime proximaConsultaEm);

    void atualizarUltimoJob(Long idJob, String tipoDocumento, String terminalNome,
                            String impressoraSolicitada, String impressoraUsada);

    AgentStateDTO obterEstado();
}
