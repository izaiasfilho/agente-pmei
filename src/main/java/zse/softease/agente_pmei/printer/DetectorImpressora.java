package zse.softease.agente_pmei.printer;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class DetectorImpressora {

    public PrintService detectarImpressoraPadrao() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (services == null || services.length == 0) {
            throw new IllegalStateException("Nenhuma impressora encontrada no sistema");
        }

        // regra simples inicial: primeira Elgin
        for (PrintService service : services) {
            String nome = service.getName().toLowerCase();

            if (nome.contains("elgin")) {
                return service;
            }
        }

        // fallback: primeira impressora disponível
        return services[0];
    }
}
