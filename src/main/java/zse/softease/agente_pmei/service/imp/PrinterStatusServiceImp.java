package zse.softease.agente_pmei.service.imp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.Attribute;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.db.AgentDatabase;
import zse.softease.agente_pmei.dto.PrinterStatusDTO;
import zse.softease.agente_pmei.service.PrinterStatusService;

@Service
public class PrinterStatusServiceImp implements PrinterStatusService {

    private static final String DB_URL = AgentDatabase.url();

    @Override
    public List<PrinterStatusDTO> obterStatus() {

        List<PrinterStatusDTO> lista = new ArrayList<>();

        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (services == null || services.length == 0) {
            salvarStatus("NENHUMA", false, "NAO_ENCONTRADA", null, null);
            return lista;
        }

        for (PrintService ps : services) {

            boolean isDefault = defaultPrinter != null &&
                                ps.getName().equals(defaultPrinter.getName());

            String status = "ONLINE";

            Attribute attr = ps.getAttribute(PrinterIsAcceptingJobs.class);
            if (attr != null && attr.toString().contains("not")) {
                status = "OFFLINE";
            }

            PrinterStatusDTO dto = new PrinterStatusDTO(
                    ps.getName(),
                    isDefault,
                    status,
                    null,
                    ps.getAttribute(javax.print.attribute.standard.PrinterInfo.class) != null
                            ? ps.getAttribute(javax.print.attribute.standard.PrinterInfo.class).toString()
                            : null,
                    LocalDateTime.now()
            );

            lista.add(dto);

            salvarStatus(
                    dto.nome(),
                    dto.defaultPrinter(),
                    dto.status(),
                    dto.porta(),
                    dto.driver()
            );
        }

        return lista;
    }

    private void salvarStatus(String nome,
                              boolean isDefault,
                              String status,
                              String porta,
                              String driver) {

        String sql = """
            INSERT INTO printer_status (nome, is_default, status, porta, driver)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nome);
            ps.setBoolean(2, isDefault);
            ps.setString(3, status);
            ps.setString(4, porta);
            ps.setString(5, driver);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar status da impressora", e);
        }
    }
}
