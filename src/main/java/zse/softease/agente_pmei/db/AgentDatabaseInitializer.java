package zse.softease.agente_pmei.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
public class AgentDatabaseInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        try {
            Path dbPath = AgentDatabase.path();

            criarBancoSeNaoExistir(dbPath);
            criarEstrutura();
            inserirDefaultsSeNecessario();

            System.out.println("[AGENTE] Banco SQLite pronto em: " + dbPath.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar banco do agente", e);
        }
    }

    // ================= BANCO =================

    private void criarBancoSeNaoExistir(Path dbPath) throws Exception {
        if (!Files.exists(dbPath)) {
            Files.createFile(dbPath);
        }

        try (Connection ignored = DriverManager.getConnection(AgentDatabase.url())) {
            // garante criação do banco
        }
    }

    // ================= ESTRUTURA =================

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

    // ================= DEFAULTS =================

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
                ('apiBaseUrl', 'http://127.0.0.1:8080/posmei-api/api/posmei/impressao'),
                ('idCaixa', ''),
                ('chaveAcesso', ''),
                ('intervaloMs', '100000');
            """);
        }
    }

    // ================= SQL =================

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
            atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP
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
