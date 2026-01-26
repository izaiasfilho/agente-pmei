package zse.softease.agente_pmei.controller;

import org.springframework.web.bind.annotation.*;
import zse.softease.agente_pmei.dto.PrinterStatusDTO;
import zse.softease.agente_pmei.service.PrinterStatusService;

import java.util.List;

@RestController
@RequestMapping("/api/agent/printer")
public class PrinterController {

    private final PrinterStatusService printerStatusService;

    public PrinterController(PrinterStatusService printerStatusService) {
        this.printerStatusService = printerStatusService;
    }

    @GetMapping("/status")
    public List<PrinterStatusDTO> status() {
        return printerStatusService.obterStatus();
    }
}
