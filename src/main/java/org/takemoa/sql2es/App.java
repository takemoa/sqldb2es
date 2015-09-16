package org.takemoa.sql2es;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.takemoa.sql2es.channel.ChannelManager;
import org.takemoa.sql2es.config.ConfigManager;
import org.takemoa.sql2es.es.ESClientManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application main entry point
 *
 * @author Take Moa
 */
public class App implements Runnable {

    private static final Logger logger = LogManager.getLogger(App.class);

    private ConfigManager configManager = null;

    public static void main(String[] args) throws JsonParseException,
		JsonMappingException, IOException {
        logger.info("Starting SQL to ElasticSearch process ...");

        App app = new App();
        int runIntervalMins = app.configManager.getRunIntervalMins();

        if (runIntervalMins <= 0) {
            logger.info("Running application only once");
            app.run();
        } else {
            logger.info("Running application every {} mins", runIntervalMins);
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(app, 0, runIntervalMins, TimeUnit.MINUTES);
        }
	}

    private App() {
        init();
    }

    private void init() {
        // 1. Load and validate application config file
        configManager = new ConfigManager();
        configManager.setup();
    }

    public void run() {
        try {
            // 2. Process/execute each channel
            for (ChannelManager channelManager : configManager.getChannelManagers()) {
                channelManager.execute();
            }
        } catch (Exception e) {
            logger.error("App level error", e);
            throw new RuntimeException(e);
        } finally {
            // 3. Cleanup
            ESClientManager.closeAll();
        }
    }
}
