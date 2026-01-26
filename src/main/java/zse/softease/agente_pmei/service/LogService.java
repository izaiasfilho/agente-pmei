package zse.softease.agente_pmei.service;

import java.util.List;

import zse.softease.agente_pmei.dto.AgentLogDTO;

public interface LogService {

    void info(String mensagem);

    void erro(String mensagem, String detalhe);

    List<AgentLogDTO> ultimos(int limite);
}
