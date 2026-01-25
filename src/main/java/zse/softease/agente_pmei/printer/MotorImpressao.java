package zse.softease.agente_pmei.printer;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;

import org.springframework.stereotype.Component;

@Component
public class MotorImpressao {

    private final DetectorImpressora detector = new DetectorImpressora();

    public void printRawBytes(byte[] data, String jobName) {
        try {
            PrintService printer = detector.detectarImpressoraPadrao();

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(data, flavor, null);

            HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new JobName(jobName, null));

            job.print(doc, attrs);

        } catch (PrintException e) {
            throw new RuntimeException("Falha ao imprimir", e);
        }
    }
}
