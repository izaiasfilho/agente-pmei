package zse.softease.agente_pmei.service;

import java.util.List;

import zse.softease.agente_pmei.dto.PrinterStatusDTO;

public interface PrinterStatusService {

    List<PrinterStatusDTO> obterStatus();
}
