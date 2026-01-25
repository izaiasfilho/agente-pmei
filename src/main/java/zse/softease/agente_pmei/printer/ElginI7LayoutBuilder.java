package zse.softease.agente_pmei.printer;


import java.nio.charset.StandardCharsets;
public class ElginI7LayoutBuilder {

    public byte[] buildTesteCupom() {
        StringBuilder sb = new StringBuilder();

        sb.append("\u001B@");
        sb.append("\u001B\u0061\u0001");
        sb.append("POSMEI\n\n");
        sb.append("\u001B\u0061\u0000");
        sb.append("TESTE DE IMPRESSÃO\n");
        sb.append("-------------------------------\n");
        sb.append("Produto A      1 x 10,00\n");
        sb.append("Produto B      2 x  5,00\n");
        sb.append("-------------------------------\n");
        sb.append("TOTAL:        R$ 20,00\n\n");
        sb.append("\u001B\u0061\u0001");
        sb.append("OBRIGADO!\n\n\n");
        sb.append("\u001D\u0056\u0041\u0010");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
