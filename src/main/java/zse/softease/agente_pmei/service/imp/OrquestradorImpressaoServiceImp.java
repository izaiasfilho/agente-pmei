package zse.softease.agente_pmei.service.imp;

import java.util.Base64;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.client.ImpressaoApiClientBack;
import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.ProximoJobResponse;
import zse.softease.agente_pmei.printer.MotorImpressao;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

/*############## ORQUESTRA A CONVERSA ENTRE BACK E IMPRESSORA*/
@Service
public class OrquestradorImpressaoServiceImp implements OrquestradorImpressaoService {

    private final ImpressaoApiClientBack impressaoApiClientBack;
    private final MotorImpressao motorImpressao;
    private final ConfiguracaoAgente configuracaoAgente;
    private final LogService logService;

    public OrquestradorImpressaoServiceImp(
            ConfiguracaoAgente configuracaoAgente,
            MotorImpressao motorImpressao,
            LogService logService,
            ImpressaoApiClientBack impressaoApiClientBack
    ) {
        this.configuracaoAgente = configuracaoAgente;
        this.motorImpressao = motorImpressao;
        this.logService = logService;
        this.impressaoApiClientBack = impressaoApiClientBack;
    }

    @Override
    public void executarCiclo() {

        try {

            // 🔒 Validação de configuração mínima
            if (configuracaoAgente.getIdCaixa() == null ||
                configuracaoAgente.getChaveAcesso() == null ||
                configuracaoAgente.getChaveAcesso().isBlank() ||
                configuracaoAgente.getApiBaseUrl() == null ||
                configuracaoAgente.getApiBaseUrl().isBlank()) {

                logService.info("Configuração incompleta. Aguardando preenchimento...");
                return;
            }

            // 🔎 Busca próximo job
            ProximoJobResponse job =
                    impressaoApiClientBack.buscarProximoJob(
                            configuracaoAgente.getIdCaixa(),
                            configuracaoAgente.getChaveAcesso()
                    );

            if (job == null) {
                return; // nenhum job disponível
            }

            logService.info("Job recebido: " + job.idJob);

            try {

                // 📦 Decodifica Base64
                byte[] dados = Base64.getDecoder().decode(job.conteudo);

                // 🖨️ Envia para impressora
                motorImpressao.printRawBytes(
                        dados,
                        "POSMEI-" + job.tipoDocumento
                );

                // ✅ Confirma sucesso
                impressaoApiClientBack.confirmarJob(job.idJob, "OK", null);

                logService.info("Job impresso com sucesso: " + job.idJob);

            } catch (Exception erroImpressao) {

                logService.erro(
                        "Erro ao imprimir job " + job.idJob,
                        erroImpressao.getMessage()
                );

                // ❌ Confirma erro ao backend
                impressaoApiClientBack.confirmarJob(
                        job.idJob,
                        "ERRO",
                        erroImpressao.getMessage()
                );
            }

        } catch (Exception erroGeral) {

            logService.erro(
                    "Erro no ciclo do agente",
                    erroGeral.getMessage()
            );

            System.err.println("[AGENTE] erro polling: " + erroGeral.getMessage());
        }
    }
}
