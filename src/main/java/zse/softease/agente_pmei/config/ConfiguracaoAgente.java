package zse.softease.agente_pmei.config;

import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.service.ConfigServiceSQLite;

@Component
public class ConfiguracaoAgente {

    private final ConfigServiceSQLite configService;

    public ConfiguracaoAgente(ConfigServiceSQLite configService) {
        this.configService = configService;
    }

    public String getChaveAgente() {
        return configService.get("chaveAgente");
    }

    public String getApiBaseUrl() {
        return configService.get("apiBaseUrl");
    }

    public String getImpressoraFallback() {
        return configService.get("impressoraFallback");
    }

    public Integer getLarguraPapelPadraoMm() {
        return configService.getInt("larguraPapelPadraoMm", 80);
    }

    public boolean isUsarNomeImpressoraDoJob() {
        return !"false".equals(configService.get("usarNomeImpressoraDoJob"));
    }

    public boolean isPermitirFallbackSistema() {
        return "true".equals(configService.get("permitirFallbackSistema"));
    }

    public boolean isModoTecnicoHabilitado() {
        return "true".equals(configService.get("modoTecnicoHabilitado"));
    }

    // Kept for migration compatibility — legacy agents may still have these values
    public Long getIdCaixa() {
        String v = configService.get("idCaixa");
        return v != null && !v.isBlank() ? Long.parseLong(v) : null;
    }

    public String getChaveAcesso() {
        return configService.get("chaveAcesso");
    }
}
