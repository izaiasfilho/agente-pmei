package zse.softease.agente_pmei.service.imp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.config.ConfiguracaoAgente;
import zse.softease.agente_pmei.db.AgentDatabase;
import zse.softease.agente_pmei.dto.AgentStateDTO;
import zse.softease.agente_pmei.service.AgentStateService;

@Service
public class AgentStateServiceSQLite implements AgentStateService {

    private static final String DB_URL = AgentDatabase.url();

    private final ConfiguracaoAgente configuracaoAgente;

    public AgentStateServiceSQLite(ConfiguracaoAgente configuracaoAgente) {
        this.configuracaoAgente = configuracaoAgente;
    }

    @Override
    public void marcarRodando(String mensagem) {
        atualizarEstado("RODANDO", mensagem);
    }

    @Override
    public void marcarErro(String mensagem) {
        atualizarEstado("ERRO", mensagem);
    }

    @Override
    public void atualizarUltimaExecucao() {
        String sql = """
            UPDATE agent_estado
            SET ultima_execucao = CURRENT_TIMESTAMP,
                atualizado_em = CURRENT_TIMESTAMP
            WHERE id = 1
        """;
        executar(sql);
    }

    @Override
    public void atualizarPollingInfo(int ciclosSemJob, int proximoPollingMs, LocalDateTime proximaConsultaEm) {
        String sql = """
            UPDATE agent_estado
            SET ciclos_sem_job = ?,
                proximo_polling_ms = ?,
                proxima_consulta_em = ?,
                atualizado_em = CURRENT_TIMESTAMP
            WHERE id = 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ciclosSemJob);
            ps.setInt(2, proximoPollingMs);
            ps.setString(3, proximaConsultaEm != null ? proximaConsultaEm.toString() : null);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar info de polling", e);
        }
    }

    @Override
    public void atualizarUltimoJob(Long idJob, String tipoDocumento, String terminalNome,
                                   String impressoraSolicitada, String impressoraUsada) {
        String sql = """
            UPDATE agent_estado
            SET ultimo_job_id = ?,
                ultimo_tipo_documento = ?,
                ultimo_terminal = ?,
                ultima_impressora_solicitada = ?,
                ultima_impressora_usada = ?,
                atualizado_em = CURRENT_TIMESTAMP
            WHERE id = 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, idJob);
            ps.setString(2, tipoDocumento);
            ps.setString(3, terminalNome);
            ps.setString(4, impressoraSolicitada);
            ps.setString(5, impressoraUsada);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar último job", e);
        }
    }

    @Override
    public AgentStateDTO obterEstado() {
        String sql = """
            SELECT status, ultima_execucao, ultima_mensagem,
                   ciclos_sem_job, proximo_polling_ms, proxima_consulta_em,
                   ultimo_job_id, ultimo_tipo_documento, ultimo_terminal,
                   ultima_impressora_solicitada, ultima_impressora_usada
            FROM agent_estado WHERE id = 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;

            AgentStateDTO dto = new AgentStateDTO();
            dto.status = rs.getString("status");
            dto.ultimaExecucao = rs.getTimestamp("ultima_execucao") != null
                    ? rs.getTimestamp("ultima_execucao").toLocalDateTime() : null;
            dto.ultimaMensagem = rs.getString("ultima_mensagem");
            dto.ciclosSemJob = rs.getInt("ciclos_sem_job");
            dto.proximoPollingMs = rs.getInt("proximo_polling_ms");

            String proximaStr = rs.getString("proxima_consulta_em");
            dto.proximaConsultaEm = proximaStr != null ? LocalDateTime.parse(proximaStr) : null;

            dto.ultimoJobId = rs.getObject("ultimo_job_id") != null ? rs.getLong("ultimo_job_id") : null;
            dto.ultimoTipoDocumento = rs.getString("ultimo_tipo_documento");
            dto.ultimoTerminal = rs.getString("ultimo_terminal");
            dto.ultimaImpressoraSolicitada = rs.getString("ultima_impressora_solicitada");
            dto.ultimaImpressoraUsada = rs.getString("ultima_impressora_usada");

            // Enrich with live config data
            String chave = configuracaoAgente.getChaveAgente();
            dto.agenteConfigurado = chave != null && !chave.isBlank();
            dto.chaveAgenteMascarada = mascarar(chave);
            dto.apiBaseUrl = configuracaoAgente.getApiBaseUrl();

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter estado do agente", e);
        }
    }

    private void atualizarEstado(String status, String mensagem) {
        String sql = """
            UPDATE agent_estado
            SET status = ?, ultima_mensagem = ?, atualizado_em = CURRENT_TIMESTAMP
            WHERE id = 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, mensagem);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar estado do agente", e);
        }
    }

    private void executar(String sql) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar SQL: " + sql, e);
        }
    }

    private String mascarar(String chave) {
        if (chave == null || chave.isBlank()) return null;
        if (chave.length() <= 8) return "****";
        return chave.substring(0, 4) + "****" + chave.substring(chave.length() - 4);
    }
}
