package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.TimeZone;

/**
 * ES channel definition to be loaded from a config file
 * 
 * @author Take Moa
 */
public class ChannelDefinition {
	
	// Channel name
	private String name = null;

	// The target ES cluster name
	@JsonProperty("esCluster")
	private String esClusterName = null;
	// The target ES index
	private String esIndex = null;
	// The target ES type
	private String esType = null;
	@JsonProperty("datasource")
	private String datasourceName = null;
    @JsonProperty("dbTimeZoneId")
	private String dbTimeZoneId = null;

	// The domain definition file
	private String domainDefinitionFile = null; // same as ES type

	// Maximum number of records to be processed in one run
	private int maxRecords = 1000000; // same as ES type

	// The batch size
	private int batchSize = -1;

	public ChannelDefinition() {
		super();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}



	public String getEsIndex() {
		return esIndex;
	}

	public void setEsIndex(String esIndex) {
		this.esIndex = esIndex;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public String getEsType() {
		return esType;
	}

	public void setEsType(String esType) {
		this.esType = esType;
	}

	public String getDbTimeZoneId() {
		return dbTimeZoneId;
	}

	public void setDbTimeZoneId(String dbTimeZoneId) {
		this.dbTimeZoneId = dbTimeZoneId;
	}

    @JsonIgnore
    public TimeZone getDbTimeZone() {
        if (dbTimeZoneId == null) {
            return null;
        }

        return TimeZone.getTimeZone(dbTimeZoneId);
    }

	public String getDomainDefinitionFile() {
		return domainDefinitionFile;
	}

	public void setDomainDefinitionFile(String domainDefinitionFile) {
		this.domainDefinitionFile = domainDefinitionFile;
	}

	public int getMaxRecords() {
		return maxRecords;
	}

	public void setMaxRecords(int maxRecords) {
		this.maxRecords = maxRecords;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
	
	public String getEsClusterName() {
		return esClusterName;
	}

	public void setEsClusterName(String esClusterName) {
		this.esClusterName = esClusterName;
	}

	@Override
	public String toString() {
		return "ChannelDefinition{" +
				"name='" + name + '\'' +
				", esClusterName='" + esClusterName + '\'' +
				", esIndex='" + esIndex + '\'' +
				", esType='" + esType + '\'' +
				", datasourceName='" + datasourceName + '\'' +
				", dbTimeZoneId='" + dbTimeZoneId + '\'' +
				", domainDefinitionFile='" + domainDefinitionFile + '\'' +
				", maxRecords=" + maxRecords +
				", batchSize=" + batchSize +
				'}';
	}
}
