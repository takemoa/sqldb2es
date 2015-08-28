package org.takemoa.sql2es;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.takemoa.sql2es.channel.ChannelManager;
import org.takemoa.sql2es.config.ConfigManager;
import org.takemoa.sql2es.es.ESClientManager;

import java.io.IOException;

/**
 * Application main entry point
 *
 * @author Take Moa
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

	public static void main(String[] args) throws JsonParseException,
		JsonMappingException, IOException {
        logger.info("Starting SQL to ElasticSearch process ...");

        try {
            // 1. Load and validate application config file
            ConfigManager configManager = new ConfigManager();
            configManager.setup();

            // 2. Process/execute each channel
            for (ChannelManager channelManager : configManager.getChannelManagers()) {
                channelManager.execute();
            }
        } catch (Exception e) {
            logger.error("App level error", e);
        } finally {
            // 3. Cleanup
            ESClientManager.closeAll();
        }
	}
}
