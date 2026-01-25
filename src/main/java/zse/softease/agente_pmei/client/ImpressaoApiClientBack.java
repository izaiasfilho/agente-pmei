package zse.softease.agente_pmei.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import zse.softease.agente_pmei.dto.ApiResponseWrapper;
import zse.softease.agente_pmei.dto.ConfirmarRequest;
import zse.softease.agente_pmei.dto.ProximoJobResponse;

/*############## CONVERSA COM BACK*/
public class ImpressaoApiClientBack {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public ImpressaoApiClientBack(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ProximoJobResponse buscarProximoJob(Long idCaixa, String chaveAcesso) throws Exception {

        String endpoint = baseUrl + "/proximo?idCaixa=" + idCaixa +
                "&chaveAcesso=" + URLEncoder.encode(chaveAcesso, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");

        try (InputStream in = conn.getInputStream()) {
            ApiResponseWrapper<ProximoJobResponse> wrapper =
                    mapper.readValue(in, new TypeReference<>() {});
            return wrapper != null && wrapper.isOk() ? wrapper.data : null;
        }
    }

    public void confirmarJob(Long idJob, String status, String mensagemErro) throws Exception {

        String endpoint = baseUrl + "/confirma";
        ConfirmarRequest req = new ConfirmarRequest(idJob, status, mensagemErro);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(mapper.writeValueAsBytes(req));
        }

        conn.getInputStream().close();
    }
}
