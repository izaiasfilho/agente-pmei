package zse.softease.agente_pmei.dto;

import java.time.LocalDateTime;

public class ConfirmarRequest {
    public Long idJob;
    public String status;
    public String mensagemErro;
    public String nomeImpressoraUsada;
    public LocalDateTime dataHoraLocal;

    public ConfirmarRequest(Long idJob, String status, String mensagemErro, String nomeImpressoraUsada) {
        this.idJob = idJob;
        this.status = status;
        this.mensagemErro = mensagemErro;
        this.nomeImpressoraUsada = nomeImpressoraUsada;
        this.dataHoraLocal = LocalDateTime.now();
    }
}
