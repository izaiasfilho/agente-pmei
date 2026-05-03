package zse.softease.agente_pmei.service;

public interface OrquestradorImpressaoService {
    // Returns null if no job found; returns proximoPollingMs hint (>=0) if job was processed
    Integer executarCiclo();
}
