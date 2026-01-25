package zse.softease.agente_pmei.printer;


import javax.print.*;
import java.nio.charset.StandardCharsets;

public class ElginI7Printer {

    public void printTest() {
        try {
            PrintService printer = findPrinter();

            if (printer == null) {
                throw new RuntimeException("Impressora Elgin não encontrada");
            }

            byte[] data = buildTestCupom();

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(data, flavor, null);

            job.print(doc, null);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao imprimir", e);
        }
    }

    private PrintService findPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService service : services) {
            if (service.getName().toLowerCase().contains("elgin")) {
                return service;
            }
        }
        return null;
    }

    private byte[] buildTestCupom() {
        StringBuilder sb = new StringBuilder();

        sb.append("\u001B@");            // init
        sb.append("\u001B\u0061\u0001"); // centralizado
        sb.append("POSMEI\n");
        sb.append("\n");

        sb.append("\u001B\u0061\u0000"); // esquerda
        sb.append("TESTE DE IMPRESSÃO\n");
        sb.append("-------------------------------\n");
        sb.append("Produto A      1 x 10,00\n");
        sb.append("Produto B      2 x  5,00\n");
        sb.append("-------------------------------\n");
        sb.append("TOTAL:        R$ 20,00\n");
        sb.append("\n");

        sb.append("\u001B\u0061\u0001");
        sb.append("OBRIGADO!\n\n\n");

        sb.append("\u001D\u0056\u0041\u0010"); // GS V A (corte)

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
