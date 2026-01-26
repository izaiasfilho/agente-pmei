package zse.softease.agente_pmei.service;

import java.util.Map;

public interface ConfigServiceSQLite {

	    String get(String chave);

	    Integer getInt(String chave, Integer defaultValue);

	    void set(String chave, String valor);
	    
	    Map<String, String> getAll();
}
