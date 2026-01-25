package zse.softease.agente_pmei.service;

public interface ConfigServiceSQLite {

	    String get(String chave);

	    Integer getInt(String chave, Integer defaultValue);

	    void set(String chave, String valor);
}
