package zse.softease.agente_pmei.dto;

public record AgentConfigDTO(
        Long idCaixa,
        String chaveAcesso,
        String apiBaseUrl,
        Integer intervaloMs
) {}
