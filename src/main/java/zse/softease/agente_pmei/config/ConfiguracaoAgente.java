package zse.softease.agente_pmei.config;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfiguracaoAgente {

	private static final String CAMINHO_CONFIG =
	        "C:/posmei-agente/config.json";


    private String apiBaseUrl;
    private String chaveAcesso;
    private Long idCaixa;
    private Long intervaloMs = 1000L;

    public static ConfiguracaoAgente carregar() {
        try {
            File file = new File(CAMINHO_CONFIG);

            if (!file.exists()) {
                throw new IllegalStateException(
                    "Arquivo de configuração não encontrado em: "
                    + file.getAbsolutePath()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            ConfiguracaoAgente config =
                    mapper.readValue(file, ConfiguracaoAgente.class);

            validar(config);

            System.out.println(
                "[AGENTE] Configuração carregada de: "
                + file.getAbsolutePath()
            );

            return config;

        } catch (Exception e) {
            throw new RuntimeException(
                "Erro ao carregar configuracao do agente", e
            );
        }
    }

    private static void validar(ConfiguracaoAgente config) {
        if (config.apiBaseUrl == null || config.apiBaseUrl.isBlank()) {
            throw new IllegalStateException("apiBaseUrl é obrigatório");
        }
        if (config.chaveAcesso == null || config.chaveAcesso.isBlank()) {
            throw new IllegalStateException("chaveAcesso é obrigatória");
        }
        if (config.idCaixa == null) {
            throw new IllegalStateException("idCaixa é obrigatório");
        }
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public Long getIdCaixa() {
        return idCaixa;
    }

    public Long getIntervaloMs() {
        return intervaloMs;
    }
}
