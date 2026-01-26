package zse.softease.agente_pmei.dto;

import java.time.LocalDateTime;

public record AgentLogDTO(
        LocalDateTime dataHora,
        String tipo,
        String mensagem,
        String detalhe
) {}
