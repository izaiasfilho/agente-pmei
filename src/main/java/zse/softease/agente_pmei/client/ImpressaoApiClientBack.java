package zse.softease.agente_pmei.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.ApiResponseWrapper;
import zse.softease.agente_pmei.dto.ConfirmarRequest;
import zse.softease.agente_pmei.dto.ProximoJobResponse;

@Component
public class ImpressaoApiClientBack {

    private final ObjectMapper mapper;
    private final ConfiguracaoAgente configuracaoAgente;

    public ImpressaoApiClientBack(ConfiguracaoAgente configuracaoAgente) {
        this.configuracaoAgente = configuracaoAgente;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    private String baseUrl() {
        String url = configuracaoAgente.getApiBaseUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("apiBaseUrl não configurada no agente");
        }
        return url;
    }

    private String chaveAgente() {
        return configuracaoAgente.getChaveAgente();
    }

    public ProximoJobResponse buscarProximoJob() throws Exception {
        String chave = chaveAgente();
        if (chave == null || chave.isBlank()) {
            return null;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + "/agente/proximo").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-Agente-Key", chave);

        int responseCode = conn.getResponseCode();
        if (responseCode == 204) return null;
        if (responseCode != 200) {
            throw new RuntimeException("Erro HTTP ao buscar job: " + responseCode);
        }

        try (InputStream in = conn.getInputStream()) {
            ApiResponseWrapper<ProximoJobResponse> wrapper =
                    mapper.readValue(in, new TypeReference<>() {});
            return wrapper != null && wrapper.isOk() ? wrapper.data : null;
        }
    }

    public void confirmarJob(Long idJob, String status, String mensagemErro, String nomeImpressoraUsada) throws Exception {
        String chave = chaveAgente();
        ConfirmarRequest req = new ConfirmarRequest(idJob, status, mensagemErro, nomeImpressoraUsada);

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + "/agente/confirma").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json");
        if (chave != null && !chave.isBlank()) {
            conn.setRequestProperty("X-Agente-Key", chave);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(mapper.writeValueAsBytes(req));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Erro HTTP ao confirmar job: " + responseCode);
        }

        conn.getInputStream().close();
    }
}
