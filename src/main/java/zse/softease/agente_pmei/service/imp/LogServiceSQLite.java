package zse.softease.agente_pmei.service.imp;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.db.AgentDatabase;
import zse.softease.agente_pmei.dto.AgentLogDTO;
import zse.softease.agente_pmei.service.LogService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogServiceSQLite implements LogService {
	private static final String DB_URL = AgentDatabase.url();


    @Override
    public void info(String mensagem) {
        inserir("INFO", mensagem, null);
    }

    @Override
    public void erro(String mensagem, String detalhe) {
        inserir("ERRO", mensagem, detalhe);
    }

    private void inserir(String tipo, String mensagem, String detalhe) {
        String sql = """
            INSERT INTO agent_log (tipo, mensagem, detalhe)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo);
            ps.setString(2, mensagem);
            ps.setString(3, detalhe);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gravar log", e);
        }
    }

    @Override
    public List<AgentLogDTO> ultimos(int limite) {
        String sql = """
            SELECT data_hora, tipo, mensagem, detalhe
            FROM agent_log
            ORDER BY data_hora DESC
            LIMIT ?
        """;

        List<AgentLogDTO> logs = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limite);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(new AgentLogDTO(
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("mensagem"),
                        rs.getString("detalhe")
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar logs", e);
        }

        return logs;
    }
}
