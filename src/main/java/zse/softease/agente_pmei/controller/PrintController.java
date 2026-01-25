package zse.softease.agente_pmei.controller;



import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import zse.softease.agente_pmei.service.PrintService;

@RestController
public class PrintController {

    private final PrintService printService;

    public PrintController(PrintService printService) {
        this.printService = printService;
    }

    @GetMapping("/print/test")
    public String printTest() {
    	printService.printTest();
        return "Teste enviado para impressora";
    }
}
