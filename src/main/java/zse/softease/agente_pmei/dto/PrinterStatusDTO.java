package zse.softease.agente_pmei.dto;

import java.time.LocalDateTime;

public record PrinterStatusDTO(
        String nome,
        boolean defaultPrinter,
        String status,
        String porta,
        String driver,
        LocalDateTime atualizadoEm
) {}
