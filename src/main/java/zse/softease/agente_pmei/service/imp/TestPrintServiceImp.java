package zse.softease.agente_pmei.service.imp;

import org.springframework.stereotype.Service;
import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.printer.MotorImpressao;
import zse.softease.agente_pmei.printer.PrintJobContext;
import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.TestPrintService;

import java.nio.charset.StandardCharsets;

@Service
public class TestPrintServiceImp implements TestPrintService {

    private final MotorImpressao motorImpressao;
    private final LogService logService;
    private final AgentStateService agentStateService;
    private final ConfiguracaoAgente configuracaoAgente;

    public TestPrintServiceImp(
            MotorImpressao motorImpressao,
            LogService logService,
            AgentStateService agentStateService,
            ConfiguracaoAgente configuracaoAgente
    ) {
        this.motorImpressao = motorImpressao;
        this.logService = logService;
        this.agentStateService = agentStateService;
        this.configuracaoAgente = configuracaoAgente;
    }

    @Override
    public void testarImpressao() {
        try {
            byte[] conteudoTeste = gerarConteudoTeste();

            PrintJobContext ctx = new PrintJobContext();
            ctx.idJob = 0L;
            ctx.tipoDocumento = "TESTE";
            ctx.terminalNome = "Teste";
            ctx.nomeImpressoraSolicitada = configuracaoAgente.getImpressoraFallback();
            ctx.larguraPapelMm = configuracaoAgente.getLarguraPapelPadraoMm();

            String impressoraUsada = motorImpressao.printRawBytes(conteudoTeste, ctx);
            logService.info("Teste de impressão realizado com sucesso na impressora: " + impressoraUsada);

        } catch (Exception e) {
            agentStateService.marcarErro("Erro no teste de impressão");
            logService.erro("Falha no teste de impressão", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private byte[] gerarConteudoTeste() {
        String texto = """
            ============================
              TESTE DE IMPRESSAO POSMEI
            ============================

            Impressora detectada
            Comunicacao OK
            Agente operacional

            Data/Hora: %s

            ----------------------------
            """.formatted(java.time.LocalDateTime.now());

        return texto.getBytes(StandardCharsets.ISO_8859_1);
    }
}
