package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Describes an ES field
 * @author Take Moa
 */
public class FieldDefinition {

/*
 * 
    hsCode:
      sqlExpression: "cat.hscode"
      type: STRING
      mappings:
        index: "not_analyzed"
        store: "yes"
 * 
 */
	@JsonIgnore
	private String fieldName = null;
    @JsonIgnore
    private String parentTypePath = null;
    @JsonIgnore
    private String fieldPath = null;

	private String sqlExpression = null;
	@JsonProperty("type")
	private FieldType fieldType = null;
	// ES Style mappings
	private Map<String, String> mappings = null;

	public FieldDefinition() {
		super();
	}

    /**
     * Must be called when initializing
     */
    public void init(String fieldName, String parentTypePath) {
        this.fieldName = fieldName;
        this.parentTypePath = parentTypePath;
        this.fieldPath = fieldName;
        if (parentTypePath != null) {
            this.fieldPath = parentTypePath + TypeDefinition.TYPE_NAME_SEP + fieldName;
        }
    }

	public String getFieldName() {
		return fieldName;
	}

    public String getParentTypePath() {
        return parentTypePath;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public String getSqlExpression() {
		return sqlExpression;
	}

	public void setSqlExpression(String dbField) {
		this.sqlExpression = dbField;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		return "FieldDefinition{" +
				"fieldName='" + fieldName + '\'' +
				", sqlExpression='" + sqlExpression + '\'' +
				", fieldType=" + fieldType +
				", mappings=" + mappings +
				'}';
	}
}
