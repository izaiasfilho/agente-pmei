package zse.softease.agente_pmei.service.imp;

import org.springframework.stereotype.Service;
import zse.softease.agente_pmei.printer.MotorImpressao;
import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.TestPrintService;

import java.nio.charset.StandardCharsets;

@Service
public class TestPrintServiceImp implements TestPrintService {

    private final MotorImpressao motorImpressao;
    private final LogService logService;
    private final AgentStateService agentStateService;

    public TestPrintServiceImp(
            MotorImpressao motorImpressao,
            LogService logService,
            AgentStateService agentStateService
    ) {
        this.motorImpressao = motorImpressao;
        this.logService = logService;
        this.agentStateService = agentStateService;
    }

    @Override
    public void testarImpressao() {

        try {
            byte[] conteudoTeste = gerarConteudoTeste();

            motorImpressao.printRawBytes(
                    conteudoTeste,
                    "TESTE-IMPRESSAO"
            );

            logService.info("Teste de impressão realizado com sucesso");

        } catch (Exception e) {

            agentStateService.marcarErro("Erro no teste de impressão");
            logService.erro("Falha no teste de impressão", e.getMessage());

            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private byte[] gerarConteudoTeste() {
        String texto = """
            ============================
              TESTE DE IMPRESSÃO POSMEI
            ============================

            ✔ Impressora detectada
            ✔ Comunicação OK
            ✔ Agente operacional

            Data/Hora: %s

            ----------------------------
            """.formatted(java.time.LocalDateTime.now());

        return texto.getBytes(StandardCharsets.ISO_8859_1);
    }
}
