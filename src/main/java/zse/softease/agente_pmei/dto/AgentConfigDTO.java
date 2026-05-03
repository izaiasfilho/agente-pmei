package zse.softease.agente_pmei.dto;

public record AgentConfigDTO(
        String chaveAgente,
        String apiBaseUrl,
        String impressoraFallback,
        Integer larguraPapelPadraoMm,
        Boolean usarNomeImpressoraDoJob,
        Boolean permitirFallbackSistema,
        Boolean modoTecnicoHabilitado
) {}
