package zse.softease.agente_pmei.controller;



import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintController {

  //  private final ExecutorImpressaoService printService;

    public PrintController() {
       // this.printService = printService;
    }

    @GetMapping("/print/test")
    public String printTest() {
    	//printService.printTest();
        return "Teste enviado para impressora";
    }
}
