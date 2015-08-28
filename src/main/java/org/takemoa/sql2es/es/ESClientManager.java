package org.takemoa.sql2es.es;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.takemoa.sql2es.config.Registry;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Take Moa
 *
 */
public class ESClientManager {

	private static HashMap<String, Node> esNodeMap = new HashMap<String, Node>();
	
	/**
	 * Retrieve a client for the input ES cluser
	 * @param esClusterName
	 * @return
	 */
	public static Client get(String esClusterName) {
		synchronized (esNodeMap) {
			Node node = esNodeMap.get(esClusterName);
			if (node == null) {
				node = createClientNode(esClusterName);
				esNodeMap.put(esClusterName, node);
			} else if (node.isClosed()) {
				node.start();
			}
			return node.client();
		}
	}
	
	public static void close(String esClusterName) {
		synchronized (esNodeMap) {
			Node node = esNodeMap.get(esClusterName);
			if (node != null) {
				node.close();
				esNodeMap.remove(esClusterName);
			}
		}
	}
	
	public static void closeAll() {
		synchronized (esNodeMap) {
			for (Node node: esNodeMap.values()) {
				node.close();
			}
			esNodeMap.clear();;
		}
	}

	private static Node createClientNode(String esClusterName) {
		// TODO can we set a specific name???

        // Get cluster settings from config
        Map<String, String> esClusterSettings = Registry.getConfig().getEsClusterSettings(esClusterName);
        // Create settings
        ImmutableSettings.Builder builder = ImmutableSettings.builder();
        builder.put("cluster.name", esClusterName);
        if (esClusterSettings != null) {
            builder.put(esClusterSettings);
        }

        // And the node
        Node node = NodeBuilder.nodeBuilder().client(true).settings(builder).node();

		return node;
	}
}
