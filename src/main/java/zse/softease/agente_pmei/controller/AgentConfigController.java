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

    @GetMapping
    public AgentConfigDTO getConfig() {
        return new AgentConfigDTO(
                configuracaoAgente.getChaveAgente(),
                configuracaoAgente.getApiBaseUrl(),
                configuracaoAgente.getImpressoraFallback(),
                configuracaoAgente.getLarguraPapelPadraoMm(),
                configuracaoAgente.isUsarNomeImpressoraDoJob(),
                configuracaoAgente.isPermitirFallbackSistema(),
                configuracaoAgente.isModoTecnicoHabilitado()
        );
    }

    @PostMapping
    public void salvar(@RequestBody AgentConfigDTO dto) {
        if (dto.chaveAgente() != null)
            configService.set("chaveAgente", dto.chaveAgente());

        if (dto.apiBaseUrl() != null)
            configService.set("apiBaseUrl", dto.apiBaseUrl());

        if (dto.impressoraFallback() != null)
            configService.set("impressoraFallback", dto.impressoraFallback());

        if (dto.larguraPapelPadraoMm() != null)
            configService.set("larguraPapelPadraoMm", dto.larguraPapelPadraoMm().toString());

        if (dto.usarNomeImpressoraDoJob() != null)
            configService.set("usarNomeImpressoraDoJob", dto.usarNomeImpressoraDoJob().toString());

        if (dto.permitirFallbackSistema() != null)
            configService.set("permitirFallbackSistema", dto.permitirFallbackSistema().toString());

        if (dto.modoTecnicoHabilitado() != null)
            configService.set("modoTecnicoHabilitado", dto.modoTecnicoHabilitado().toString());
    }

    @PostMapping("/test-connection")
    public String testarConexao() {
        try {
            return "Conexão com o back OK";
        } catch (Exception e) {
            return "Erro ao conectar no back: " + e.getMessage();
        }
    }

    @PostMapping("/test-print")
    public TestPrintResponseDTO testarImpressao() {
        try {
            testPrintService.testarImpressao();
            return new TestPrintResponseDTO(true, "Teste de impressão enviado com sucesso");
        } catch (Exception e) {
            return new TestPrintResponseDTO(false, "Erro ao testar impressão: " + e.getMessage());
        }
    }
}
