package zse.softease.agente_pmei.service.imp;

import java.util.Base64;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.client.ImpressaoApiClientBack;
import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.ProximoJobResponse;
import zse.softease.agente_pmei.printer.MotorImpressao;
import zse.softease.agente_pmei.printer.PrintJobContext;
import zse.softease.agente_pmei.service.AgentStateService;
import zse.softease.agente_pmei.service.LogService;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;

@Service
public class OrquestradorImpressaoServiceImp implements OrquestradorImpressaoService {

    private final ImpressaoApiClientBack impressaoApiClientBack;
    private final MotorImpressao motorImpressao;
    private final ConfiguracaoAgente configuracaoAgente;
    private final LogService logService;
    private final AgentStateService agentStateService;

    public OrquestradorImpressaoServiceImp(
            ConfiguracaoAgente configuracaoAgente,
            MotorImpressao motorImpressao,
            LogService logService,
            ImpressaoApiClientBack impressaoApiClientBack,
            AgentStateService agentStateService
    ) {
        this.configuracaoAgente = configuracaoAgente;
        this.motorImpressao = motorImpressao;
        this.logService = logService;
        this.impressaoApiClientBack = impressaoApiClientBack;
        this.agentStateService = agentStateService;
    }

    @Override
    public Integer executarCiclo() {

        try {
            String chaveAgente = configuracaoAgente.getChaveAgente();

            if (chaveAgente == null || chaveAgente.isBlank()) {
                logService.info("Agente não configurado (chaveAgente ausente). Status: CONFIGURACAO_PENDENTE");
                agentStateService.marcarErro("CONFIGURACAO_PENDENTE — Baixe o agente pelo sistema para ativar.");
                return null;
            }

            if (configuracaoAgente.getApiBaseUrl() == null || configuracaoAgente.getApiBaseUrl().isBlank()) {
                logService.info("apiBaseUrl não configurada. Aguardando...");
                return null;
            }

            ProximoJobResponse job = impressaoApiClientBack.buscarProximoJob();

            if (job == null) {
                return null; // no job available
            }

            logService.info("Job recebido: #" + job.idJob
                    + " | terminal=" + job.terminalNome
                    + " | tipo=" + job.tipoDocumento
                    + " | impressora=" + job.nomeImpressora);

            PrintJobContext ctx = new PrintJobContext();
            ctx.idJob = job.idJob;
            ctx.tipoDocumento = job.tipoDocumento;
            ctx.terminalNome = job.terminalNome;
            ctx.nomeImpressoraSolicitada = configuracaoAgente.isUsarNomeImpressoraDoJob()
                    ? job.nomeImpressora : null;
            ctx.larguraPapelMm = job.larguraPapelMm != null
                    ? job.larguraPapelMm : configuracaoAgente.getLarguraPapelPadraoMm();

            String nomeImpressoraUsada = null;

            try {
                byte[] dados = Base64.getDecoder().decode(job.conteudo);

                nomeImpressoraUsada = motorImpressao.printRawBytes(dados, ctx);

                impressaoApiClientBack.confirmarJob(job.idJob, "OK", null, nomeImpressoraUsada);

                logService.info("Job #" + job.idJob + " impresso com sucesso na impressora: " + nomeImpressoraUsada);

                agentStateService.atualizarUltimoJob(
                        job.idJob,
                        job.tipoDocumento,
                        job.terminalNome,
                        job.nomeImpressora,
                        nomeImpressoraUsada
                );

            } catch (Exception erroImpressao) {
                String msgErro = erroImpressao.getMessage();

                logService.erro("Erro ao imprimir job #" + job.idJob
                        + " | impressora=" + ctx.nomeImpressoraSolicitada, msgErro);

                impressaoApiClientBack.confirmarJob(job.idJob, "ERRO", msgErro, nomeImpressoraUsada);

                agentStateService.atualizarUltimoJob(
                        job.idJob,
                        job.tipoDocumento,
                        job.terminalNome,
                        job.nomeImpressora,
                        null
                );
            }

            return job.proximoPollingMs != null ? job.proximoPollingMs : 0;

        } catch (Exception erroGeral) {
            logService.erro("Erro no ciclo do agente", erroGeral.getMessage());
            System.err.println("[AGENTE] erro polling: " + erroGeral.getMessage());
            throw new RuntimeException(erroGeral.getMessage(), erroGeral);
        }
    }
}
