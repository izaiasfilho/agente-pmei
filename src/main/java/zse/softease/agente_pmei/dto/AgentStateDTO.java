package zse.softease.agente_pmei.dto;

import java.time.LocalDateTime;

public class AgentStateDTO {
    public String status;
    public LocalDateTime ultimaExecucao;
    public String ultimaMensagem;
    public String apiBaseUrl;
    public boolean agenteConfigurado;
    public String chaveAgenteMascarada;
    public int proximoPollingMs;
    public LocalDateTime proximaConsultaEm;
    public int ciclosSemJob;
    public Long ultimoJobId;
    public String ultimoTipoDocumento;
    public String ultimoTerminal;
    public String ultimaImpressoraSolicitada;
    public String ultimaImpressoraUsada;
    public String ultimoErro;

    public AgentStateDTO() {}
}
