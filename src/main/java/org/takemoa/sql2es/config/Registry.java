package org.takemoa.sql2es.config;

/**
 * Keeps the global objects/instances. The respective objects must register themselves as they are created.
 *
 * @author Take Moa
 */
public class Registry {
    private static Config config = null;
    private static ConfigManager configManager = null;

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config config) {
        Registry.config = config;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static void setConfigManager(ConfigManager configManager) {
        Registry.configManager = configManager;
    }
}
