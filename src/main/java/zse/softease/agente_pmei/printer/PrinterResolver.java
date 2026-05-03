package zse.softease.agente_pmei.printer;

import java.util.Arrays;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.springframework.stereotype.Component;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;

@Component
public class PrinterResolver {

    private final ConfiguracaoAgente configuracaoAgente;

    public PrinterResolver(ConfiguracaoAgente configuracaoAgente) {
        this.configuracaoAgente = configuracaoAgente;
    }

    public PrintService resolver(String nomeImpressora) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (services == null || services.length == 0) {
            throw new IllegalStateException("Nenhuma impressora encontrada no sistema");
        }

        // 1. Exact match
        if (nomeImpressora != null && !nomeImpressora.isBlank()) {
            for (PrintService ps : services) {
                if (ps.getName().equals(nomeImpressora)) {
                    return ps;
                }
            }
            // 2. Normalized match
            String norm = normalizar(nomeImpressora);
            for (PrintService ps : services) {
                if (normalizar(ps.getName()).equals(norm)) {
                    return ps;
                }
            }
        }

        // 3. Fallback from config
        String fallback = configuracaoAgente.getImpressoraFallback();
        if (fallback != null && !fallback.isBlank()) {
            for (PrintService ps : services) {
                if (ps.getName().equals(fallback) || normalizar(ps.getName()).equals(normalizar(fallback))) {
                    return ps;
                }
            }
        }

        // 4. System default only if explicitly allowed
        if (configuracaoAgente.isPermitirFallbackSistema()) {
            PrintService defaultPs = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPs != null) return defaultPs;
            return services[0];
        }

        // 5. Error with clear message
        String msg = nomeImpressora != null && !nomeImpressora.isBlank()
                ? "Impressora '" + nomeImpressora + "' não encontrada no Windows."
                : "Nenhuma impressora configurada para este job.";
        if (fallback != null && !fallback.isBlank()) {
            msg += " Fallback '" + fallback + "' também não encontrado.";
        }
        throw new IllegalStateException(msg);
    }

    public List<String> listarNomes() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null) return List.of();
        return Arrays.stream(services).map(PrintService::getName).toList();
    }

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
