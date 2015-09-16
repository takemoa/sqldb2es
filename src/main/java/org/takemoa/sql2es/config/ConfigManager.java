package org.takemoa.sql2es.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;
import org.takemoa.sql2es.channel.ChannelManager;
import org.takemoa.sql2es.definition.ChannelDefinition;
import org.takemoa.sql2es.definition.DatasourceDefinition;
import org.takemoa.sql2es.definition.DomainDefinition;
import org.takemoa.sql2es.definition.TypeDefinition;
import org.takemoa.sql2es.sql.SqlTemplates;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and manages config and definition files
 * 
 * @author Catalin
 *
 */
public class ConfigManager {

    private static final Logger logger = LogManager.getLogger(ConfigManager.class);

	private final Path homeFolder;
	private final Path configFolder;

	private String configFileName = "sql2es.yaml";
	
	public static final String ES_CONFIG_INDEX = "ecconfig_";
	public static final String ES_CONFIG_TYPE = "channels_";
	
	/**
	 * Main config file.
	 */
	private Config config = null;

	// List of managers for each channel defined in the config file.
	private ArrayList<ChannelManager> channelManagers = new ArrayList<ChannelManager>();

	public ConfigManager() {
		super();

		if (System.getProperty("sql2es.path.home") != null) {
			homeFolder = Paths.get(System.getProperty("sql2es.path.home"));
		} else {
			homeFolder = Paths.get(System.getProperty("user.dir"));
		}

		if (System.getProperty("sql2es.path.config") != null) {
			configFolder = Paths.get(System.getProperty("sql2es.path.config"));
		} else {
			configFolder = homeFolder.resolve("config");
		}

		if (System.getProperty("sql2es.filename.config") != null) {
			configFileName = System.getProperty("sql2es.filename.config");
		}

        Registry.setConfigManager(this);
        logger.info("\n Home folder:{} \n Config folder:{} \n Config filename:{}", homeFolder, configFolder,
                configFileName);
	}

	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	/**
	 * Load the main config file and all domain files. It throws ConfigException
	 * in case of any fatal error. It must be called explicitly at the application startup.
	 * 
	 * @throws ConfigException
	 */
	public void setup() {
		// 1. Main config
		loadConfig();

		// 2. Domain definitions
		Map<String, ChannelDefinition> channels = config.getChannels();
		// At this point they are already validated.

		for (Map.Entry<String, ChannelDefinition> channelEntry : channels
				.entrySet()) {

			ChannelDefinition channelDefinition = channelEntry.getValue();
			String filePath = channelDefinition.getDomainDefinitionFile();

			if (StringUtils.isEmpty(filePath)) {
				// Defaults to ES Type value for yaml extension
				filePath = channelDefinition.getEsType() + ".yaml";
			}


			DomainDefinition domainDefinition = loadDomainDefinition(filePath);
			ChannelManager channelManager = new ChannelManager(
					channelEntry.getKey(), this, channelDefinition,
					domainDefinition);

			logger.debug("\n {}", channelManager);

			channelManagers.add(channelManager);
		}
	}

	public ArrayList<ChannelManager> getChannelManagers() {
		return channelManagers;
	}

	/**
	 * Load main config file and the other files.
	 */
	public void loadConfig() {
		// Config file first
		URL configFileUrl = resolveConfigFile(configFileName);

        logger.info("Loading main configuration file: {}", configFileUrl);

		try {
			config = mapper.readValue(configFileUrl, Config.class);
		} catch (IOException e) {
			throw new ConfigException("Unable to load config file: "
					+ configFileUrl, e);
		}

        logger.debug("Configuration values: {}", config);

		// Validate the file content
		config.validate();
	}

	private DomainDefinition loadDomainDefinition(String filePath) {
		URL domainFileUrl = resolveConfigFile(filePath);
		LinkedHashMap<String, TypeDefinition> typesMap = null;

        logger.info("Loading domain definition file: {}", domainFileUrl);

		try {
			// Parse YAML domain file
			typesMap = mapper.readValue(domainFileUrl,
					new TypeReference<LinkedHashMap<String, TypeDefinition>>() {
					});
		} catch (IOException e) {
			throw new ConfigException("Unable to load domain definition file: "
					+ domainFileUrl, e);
		}

		DomainDefinition domainDefinition = new DomainDefinition(typesMap);

        logger.debug("Domain definition {}: \n{}", domainDefinition.getName(), domainDefinition);

		return domainDefinition;
	}

	/**
	 * Try to resolve the file path by looking into various folders or classpath
	 * 
	 * @param filePath
	 * @return an URL object if file could be found or NULL otherwise
	 */
	public URL resolveConfigFile(String filePath) {
		String origFilePath = filePath;

		// first, try it as a path on the file system
		Path path1 = Paths.get(filePath);
		if (Files.exists(path1)) {
			try {
				return path1.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigException("Failed to resolve path [" + path1
						+ "]", e);
			}
		}

		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		// next, try it relative to the config location
		Path path2 = configFolder.resolve(filePath);
		if (Files.exists(path2)) {
			try {
				return path2.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new ConfigException(
						"Failed to resolve path [" + path2 + "]", e);
			}
		}
		// try and load it from the classpath directly
		URL resource = ClassLoader.getSystemClassLoader().getResource(filePath);
		if (resource != null) {
			return resource;
		}
		// try and load it from the classpath with config/ prefix
		if (!filePath.startsWith("config/")) {
			resource = ClassLoader.getSystemClassLoader().getResource(
					"config/" + filePath);
			if (resource != null) {
				return resource;
			}
		}
		throw new ConfigException("Failed to resolve config path ["
				+ origFilePath + "], tried file path [" + path1
				+ "], path file [" + path2 + "], and classpath");
	}

	/**
	 * Lookup the configure SQl templates for the input SQL driver
	 * @param driverClassName
	 * @return
	 */
	public SqlTemplates getSqlTemplates(String driverClassName) {
		SqlTemplates sqlTemplates = config.getSqlTemplatesMap().get(driverClassName);
		if (sqlTemplates == null) {
			sqlTemplates = SqlTemplates.DEFAULT;
		}
		return sqlTemplates;
	}
	
	public DatasourceDefinition getDatasourceDef(String dsName) {
		return config.getDatasourceMap().get(dsName);
	}

	public int getDefaultBatchSize() {
		return config.getBatchSize();
	}

    public int getRunIntervalMins() {
        return config.getRunIntervalMins();
    }

	@Override
	public String toString() {
		return "ConfigManager [homeFolder=" + homeFolder + ", configFolder="
				+ configFolder + ", configFileName=" + configFileName
				+ ", config=" + config + ", channelManagers=" + channelManagers
				+ "]";
	}
}
