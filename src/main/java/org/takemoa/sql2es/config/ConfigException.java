package org.takemoa.sql2es.config;

/**
 * Specialized runtime excepion
 */
public class ConfigException extends RuntimeException {

	public ConfigException() {
		super();
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(Throwable cause) {
		super(cause);
	}

}
