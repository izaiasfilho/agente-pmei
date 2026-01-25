package zse.softease.agente_pmei.config;

import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.service.ConfigServiceSQLite;

@Component
public class ConfiguracaoAgente {

    private final ConfigServiceSQLite configService;

    public ConfiguracaoAgente(ConfigServiceSQLite configService) {
        this.configService = configService;
    }

    public Long getIdCaixa() {
        String v = configService.get("idCaixa");
        return v != null && !v.isBlank() ? Long.parseLong(v) : null;
    }

    public String getChaveAcesso() {
        return configService.get("chaveAcesso");
    }

    public String getApiBaseUrl() {
        return configService.get("apiBaseUrl");
    }

    public Integer getIntervaloMs() {
        return configService.getInt("intervaloMs", 10000);
    }
}
