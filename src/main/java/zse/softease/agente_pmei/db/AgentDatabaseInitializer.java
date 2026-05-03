package zse.softease.agente_pmei.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import zse.softease.agente_pmei.service.ConfigServiceSQLite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

@Component
@Order(1)
public class AgentDatabaseInitializer implements ApplicationRunner {

    private final ConfigServiceSQLite configService;

    public AgentDatabaseInitializer(ConfigServiceSQLite configService) {
        this.configService = configService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Path dbPath = AgentDatabase.path();
            criarBancoSeNaoExistir(dbPath);
            criarEstrutura();
            migrarColunas();
            inserirDefaultsSeNecessario();
            carregarBootstrap();

            System.out.println("[AGENTE] Banco SQLite pronto em: " + dbPath.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco do agente", e);
        }
    }

    private void criarBancoSeNaoExistir(Path dbPath) throws Exception {
        if (!Files.exists(dbPath)) {
            Files.createFile(dbPath);
        }
        try (Connection ignored = DriverManager.getConnection(AgentDatabase.url())) {}
    }

    private void criarEstrutura() throws Exception {
        try (Connection conn = DriverManager.getConnection(AgentDatabase.url());
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQL_AGENT_CONFIG);
            stmt.execute(SQL_AGENT_ESTADO);
            stmt.execute(SQL_PRINTER_STATUS);
            stmt.execute(SQL_AGENT_LOG);
            stmt.execute(SQL_AGENT_ATIVIDADE);
        }
    }

    private void migrarColunas() throws Exception {
        // Add new columns to agent_estado for existing databases (SQLite doesn't support IF NOT EXISTS for ADD COLUMN)
        String[] novasColunas = {
            "ALTER TABLE agent_estado ADD COLUMN ciclos_sem_job INTEGER DEFAULT 0",
            "ALTER TABLE agent_estado ADD COLUMN proximo_polling_ms INTEGER DEFAULT 3000",
            "ALTER TABLE agent_estado ADD COLUMN proxima_consulta_em TEXT",
            "ALTER TABLE agent_estado ADD COLUMN ultimo_job_id INTEGER",
            "ALTER TABLE agent_estado ADD COLUMN ultimo_tipo_documento TEXT",
            "ALTER TABLE agent_estado ADD COLUMN ultimo_terminal TEXT",
            "ALTER TABLE agent_estado ADD COLUMN ultima_impressora_solicitada TEXT",
            "ALTER TABLE agent_estado ADD COLUMN ultima_impressora_usada TEXT"
        };

        try (Connection conn = DriverManager.getConnection(AgentDatabase.url());
             Statement stmt = conn.createStatement()) {
            for (String sql : novasColunas) {
                try {
                    stmt.execute(sql);
                } catch (Exception ignored) {
                    // Column already exists — safe to ignore
                }
            }
        }
    }

    private void inserirDefaultsSeNecessario() throws Exception {
        try (Connection conn = DriverManager.getConnection(AgentDatabase.url());
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                INSERT OR IGNORE INTO agent_estado (id, status)
                VALUES (1, 'PARADO');
            """);

            stmt.execute("""
                INSERT OR IGNORE INTO agent_atividade (id)
                VALUES (1);
            """);

            stmt.execute("""
                INSERT OR IGNORE INTO agent_config (chave, valor) VALUES
                ('apiBaseUrl', 'https://api.zseposmei.cloud/posmei-api/api/posmei/impressao'),
                ('chaveAgente', ''),
                ('impressoraFallback', ''),
                ('larguraPapelPadraoMm', '80'),
                ('usarNomeImpressoraDoJob', 'true'),
                ('permitirFallbackSistema', 'false'),
                ('modoTecnicoHabilitado', 'false'),
                ('configInicializada', 'false');
            """);

            // Migration: if old chaveAcesso exists and chaveAgente is empty, use it
            migrarChaveAcesso(stmt);
        }
    }

    private void migrarChaveAcesso(Statement stmt) throws Exception {
        // If legacy chaveAcesso exists but new chaveAgente is empty, migrate
        String velhaChave = configService.get("chaveAcesso");
        String novaChave = configService.get("chaveAgente");

        if (velhaChave != null && !velhaChave.isBlank()
                && (novaChave == null || novaChave.isBlank())) {
            stmt.execute("INSERT OR REPLACE INTO agent_config (chave, valor) VALUES ('chaveAgente', '" + velhaChave + "')");
            System.out.println("[AGENTE] Migração: chaveAcesso → chaveAgente (compatibilidade)");
        }
    }

    private void carregarBootstrap() {
        Path bootstrapPath = Paths.get(System.getProperty("user.dir")).resolve("agent-bootstrap.json");

        if (!Files.exists(bootstrapPath)) return;

        String jaInicializado = configService.get("configInicializada");
        if ("true".equals(jaInicializado)) return;

        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> bootstrap = mapper.readValue(bootstrapPath.toFile(), Map.class);

            String apiBaseUrl = bootstrap.get("apiBaseUrl");
            String chaveAgente = bootstrap.get("chaveAgente");

            if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
                configService.set("apiBaseUrl", apiBaseUrl);
            }
            if (chaveAgente != null && !chaveAgente.isBlank()) {
                configService.set("chaveAgente", chaveAgente);
            }

            configService.set("configInicializada", "true");

            // Rename to prevent reuse
            Files.move(bootstrapPath, bootstrapPath.resolveSibling("agent-bootstrap.used.json"));

            System.out.println("[AGENTE] Bootstrap carregado. Chave configurada.");

        } catch (Exception e) {
            System.err.println("[AGENTE] Erro ao carregar bootstrap: " + e.getMessage());
        }
    }

    private static final String SQL_AGENT_CONFIG = """
        CREATE TABLE IF NOT EXISTS agent_config (
            chave TEXT PRIMARY KEY,
            valor TEXT NOT NULL,
            atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP
        );
    """;

    private static final String SQL_AGENT_ESTADO = """
        CREATE TABLE IF NOT EXISTS agent_estado (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            status TEXT NOT NULL,
            ultima_execucao DATETIME,
            ultima_mensagem TEXT,
            atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
            ciclos_sem_job INTEGER DEFAULT 0,
            proximo_polling_ms INTEGER DEFAULT 3000,
            proxima_consulta_em TEXT,
            ultimo_job_id INTEGER,
            ultimo_tipo_documento TEXT,
            ultimo_terminal TEXT,
            ultima_impressora_solicitada TEXT,
            ultima_impressora_usada TEXT
        );
    """;

    private static final String SQL_PRINTER_STATUS = """
        CREATE TABLE IF NOT EXISTS printer_status (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nome TEXT NOT NULL,
            is_default BOOLEAN,
            status TEXT,
            porta TEXT,
            driver TEXT,
            atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP
        );
    """;

    private static final String SQL_AGENT_LOG = """
        CREATE TABLE IF NOT EXISTS agent_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            data_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
            tipo TEXT,
            mensagem TEXT,
            detalhe TEXT
        );
    """;

    private static final String SQL_AGENT_ATIVIDADE = """
        CREATE TABLE IF NOT EXISTS agent_atividade (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            ultimo_job_recebido TEXT,
            ultimo_job_impresso TEXT,
            ultimo_erro TEXT,
            atualizado_em DATETIME
        );
    """;
}
