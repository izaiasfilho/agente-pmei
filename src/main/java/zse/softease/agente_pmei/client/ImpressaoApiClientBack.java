package zse.softease.agente_pmei.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.ApiResponseWrapper;
import zse.softease.agente_pmei.dto.ConfirmarRequest;
import zse.softease.agente_pmei.dto.ProximoJobResponse;

/*############## CONVERSA COM BACK*/
@Component
public class ImpressaoApiClientBack {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfiguracaoAgente configuracaoAgente;

    public ImpressaoApiClientBack(ConfiguracaoAgente configuracaoAgente) {
        this.configuracaoAgente = configuracaoAgente;
    }

    private String baseUrl() {
        String url = configuracaoAgente.getApiBaseUrl();

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("apiBaseUrl não configurada no agente");
        }

        return url;
    }

    public ProximoJobResponse buscarProximoJob(Long idCaixa, String chaveAcesso) throws Exception {

        if (idCaixa == null || chaveAcesso == null || chaveAcesso.isBlank()) {
            return null; // configuração ainda não feita
        }

        String endpoint = baseUrl() + "/proximo?idCaixa=" + idCaixa +
                "&chaveAcesso=" + URLEncoder.encode(chaveAcesso, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Erro HTTP ao buscar job: " + responseCode);
        }

        try (InputStream in = conn.getInputStream()) {
            ApiResponseWrapper<ProximoJobResponse> wrapper =
                    mapper.readValue(in, new TypeReference<>() {});
            return wrapper != null && wrapper.isOk() ? wrapper.data : null;
        }
    }

    public void confirmarJob(Long idJob, String status, String mensagemErro) throws Exception {

        String endpoint = baseUrl() + "/confirma";
        ConfirmarRequest req = new ConfirmarRequest(idJob, status, mensagemErro);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json");

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
