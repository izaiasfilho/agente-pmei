package zse.softease.agente_pmei.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.AgentConfigDTO;
import zse.softease.agente_pmei.dto.TestPrintResponseDTO;
import zse.softease.agente_pmei.service.ConfigServiceSQLite;
import zse.softease.agente_pmei.service.TestPrintService;

@RestController
@RequestMapping("/api/agent/config")
public class AgentConfigController {

    private final ConfigServiceSQLite configService;
    private final ConfiguracaoAgente configuracaoAgente;
    private final TestPrintService testPrintService;

    public AgentConfigController(ConfigServiceSQLite configService,
                                 ConfiguracaoAgente configuracaoAgente,
                                 TestPrintService testPrintService) {
        this.configService = configService;
        this.configuracaoAgente = configuracaoAgente;
        this.testPrintService = testPrintService;
    }

    // =========================
    // GET - carregar config
    // =========================
    @GetMapping
    public AgentConfigDTO getConfig() {
        return new AgentConfigDTO(
                configuracaoAgente.getIdCaixa(),
                configuracaoAgente.getChaveAcesso(),
                configuracaoAgente.getApiBaseUrl(),
                configuracaoAgente.getIntervaloMs()
        );
    }

    // =========================
    // POST/PUT - salvar config
    // =========================
    @PostMapping
    public void salvar(@RequestBody AgentConfigDTO dto) {

        if (dto.idCaixa() != null)
            configService.set("idCaixa", dto.idCaixa().toString());

        if (dto.chaveAcesso() != null)
            configService.set("chaveAcesso", dto.chaveAcesso());

        if (dto.apiBaseUrl() != null)
            configService.set("apiBaseUrl", dto.apiBaseUrl());

        if (dto.intervaloMs() != null)
            configService.set("intervaloMs", dto.intervaloMs().toString());
    }
    
    @PostMapping("/test-connection")
    public String testarConexao() {
        try {
            // chamada simples ao back (ex: ping / health)
            return "Conexão com o back OK";
        } catch (Exception e) {
            return "Erro ao conectar no back: " + e.getMessage();
        }
    }

    @PostMapping("/test-print")
    public TestPrintResponseDTO testarImpressao() {

        try {
            testPrintService.testarImpressao();
            return new TestPrintResponseDTO(
                    true,
                    "Teste de impressão enviado com sucesso"
            );

        } catch (Exception e) {
            return new TestPrintResponseDTO(
                    false,
                    "Erro ao testar impressão: " + e.getMessage()
            );
        }
    }


}
