package zse.softease.agente_pmei.dto;


public class ConfirmarRequest {

    public Long idJob;
    public String status;
    public String mensagemErro;

    public ConfirmarRequest(Long idJob, String status, String mensagemErro) {
        this.idJob = idJob;
        this.status = status;
        this.mensagemErro = mensagemErro;
    }
}
