package zse.softease.agente_pmei.db;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AgentDatabase {

    private static final String DB_NAME = "agent.db";

    private AgentDatabase() {}

    public static Path path() {
        return Paths.get(System.getProperty("user.dir")).resolve(DB_NAME);
    }

    public static String url() {
        return "jdbc:sqlite:" + path().toAbsolutePath();
    }
}
