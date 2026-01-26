package zse.softease.agente_pmei.dto;

import java.time.LocalDateTime;

public record AgentStateDTO(
        String status,
        LocalDateTime ultimaExecucao,
        String ultimaMensagem
) {}