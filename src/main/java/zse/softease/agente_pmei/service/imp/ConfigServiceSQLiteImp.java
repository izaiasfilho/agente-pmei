package zse.softease.agente_pmei.service.imp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import zse.softease.agente_pmei.db.AgentDatabase;
import zse.softease.agente_pmei.service.ConfigServiceSQLite;


@Service
public class ConfigServiceSQLiteImp implements ConfigServiceSQLite {
	private static final String DB_URL = AgentDatabase.url();


    @Override
    public String get(String chave) {
        String sql = "SELECT valor FROM agent_config WHERE chave = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chave);
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getString("valor") : null;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler config: " + chave, e);
        }
    }

    @Override
    public Integer getInt(String chave, Integer defaultValue) {
        String valor = get(chave);
        if (valor == null || valor.isBlank()) return defaultValue;
        return Integer.parseInt(valor);
    }

    @Override
    public void set(String chave, String valor) {
        String sql = """
            INSERT INTO agent_config (chave, valor)
            VALUES (?, ?)
            ON CONFLICT(chave)
            DO UPDATE SET valor = excluded.valor,
                          atualizado_em = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chave);
            ps.setString(2, valor);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar config: " + chave, e);
        }
    }

	@Override
	public Map<String, String> getAll() {
	    String sql = "SELECT chave, valor FROM agent_config";

	    Map<String, String> map = new HashMap<>();

	    try (Connection conn = DriverManager.getConnection(DB_URL);
	         PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            map.put(rs.getString("chave"), rs.getString("valor"));
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("Erro ao listar configurações", e);
	    }

	    return map;
	}

}
