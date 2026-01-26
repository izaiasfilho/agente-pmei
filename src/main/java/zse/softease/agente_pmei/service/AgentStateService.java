package zse.softease.agente_pmei.service;

import zse.softease.agente_pmei.dto.AgentStateDTO;

public interface AgentStateService {


    void marcarRodando(String mensagem);

    void marcarErro(String mensagem);

    void atualizarUltimaExecucao();

    AgentStateDTO obterEstado();
}
