package zse.softease.agente_pmei.service.imp;


import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.printer.ElginI7Printer;
import zse.softease.agente_pmei.service.PrintService;

@Service
public class PrintServiceImp implements PrintService{

    private final ElginI7Printer printer = new ElginI7Printer();

    @Override
    public void printTest() {
        printer.printTest();
    }
}
