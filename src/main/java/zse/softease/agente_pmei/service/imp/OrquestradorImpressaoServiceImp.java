package zse.softease.agente_pmei.service.imp;

import java.util.Base64;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import zse.softease.agente_pmei.client.ImpressaoApiClientBack;
import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.dto.ProximoJobResponse;
import zse.softease.agente_pmei.printer.MotorImpressao;
import zse.softease.agente_pmei.service.OrquestradorImpressaoService;


/*############## ORQUESTRA A CONVERSA ENTRE BACK E IMPRESSORA*/
@Service
public class OrquestradorImpressaoServiceImp implements OrquestradorImpressaoService {

    private ImpressaoApiClientBack impressaoApiClientBack;
    private final MotorImpressao motorImpressao;
    private final ConfiguracaoAgente configuracaoAgente;


    public OrquestradorImpressaoServiceImp(
            ConfiguracaoAgente configuracaoAgente,
            MotorImpressao motorImpressao
    ) {
        this.configuracaoAgente = configuracaoAgente;
        this.motorImpressao = motorImpressao;
    }
   
    @PostConstruct
    public void init() {
        String baseUrl = configuracaoAgente.getApiBaseUrl();
        this.impressaoApiClientBack =
                new ImpressaoApiClientBack(baseUrl);
    }

    @Override
    public void executarCiclo() {

        try {
            ProximoJobResponse job =
            		impressaoApiClientBack.buscarProximoJob(configuracaoAgente.getIdCaixa(), configuracaoAgente.getChaveAcesso());

            if (job == null) return;

            try {
            	byte[] dados = Base64.getDecoder().decode(job.conteudo);

            	motorImpressao.printRawBytes(
            	    dados,
            	    "POSMEI-" + job.tipoDocumento
            	);

            	impressaoApiClientBack.confirmarJob(job.idJob, "OK", null);
            	
            	            } catch (Exception e) {
            	                impressaoApiClientBack.confirmarJob(
            	                        job.idJob,
            	                        "ERRO",
            	                        e.getMessage()
            	                );
            	            }
        } catch (Exception e) {
            System.err.println("[AGENTE] erro polling: " + e.getMessage());
        }
    }
}
