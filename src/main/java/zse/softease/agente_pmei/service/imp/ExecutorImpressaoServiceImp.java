package zse.softease.agente_pmei.service.imp;


import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.printer.ElginI7LayoutBuilder;
import zse.softease.agente_pmei.service.ExecutorImpressaoService;

@Service
public class ExecutorImpressaoServiceImp implements ExecutorImpressaoService{

    private final ElginI7LayoutBuilder printer = new ElginI7LayoutBuilder();

    @Override
    public void printTest() {
       // printer.printTest();
    }
}
