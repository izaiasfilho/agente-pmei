package zse.softease.agente_pmei.service.imp;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.db.AgentDatabase;
import zse.softease.agente_pmei.dto.AgentStateDTO;
import zse.softease.agente_pmei.service.AgentStateService;

@Service
public class AgentStateServiceSQLite implements AgentStateService {
	
	
	private static final String DB_URL = AgentDatabase.url();



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

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar última execução", e);
        }
    }

    @Override
    public AgentStateDTO obterEstado() {
        String sql = """
            SELECT status, ultima_execucao, ultima_mensagem
            FROM agent_estado WHERE id = 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;

            return new AgentStateDTO(
                    rs.getString("status"),
                    rs.getTimestamp("ultima_execucao") != null
                            ? rs.getTimestamp("ultima_execucao").toLocalDateTime()
                            : null,
                    rs.getString("ultima_mensagem")
            );

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
}
