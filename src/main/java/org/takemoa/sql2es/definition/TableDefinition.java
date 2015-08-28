package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Take Moa
 */
public class TableDefinition {

/*
    <table_name_1>:
      as: <nickname>
	  # optional
      primaryKeyFields:
        - <field_name_from_field_list_1>
        - <field_name_from_field_list_2>
      # Join expression if required. If not the fields are added without a join clause
      join:
        type: INNER | LEFT_OUTER
        parentColumns:
        - <parentTableName>.<columns1>
        childColumns:
        - <childTableName>.<columns2>
 */

	@JsonIgnore
	private String tableName;
	@JsonProperty("as")
	private String nickname;
//	private String domain;
	
	// list of PK fields - optional
	@JsonProperty("primaryKeyFields")
	private List<String> pkFieldNames;
	@JsonProperty("join")
	private JoinDefinition joinDef;

//	@JsonProperty("refField")
//	private String referenceFieldName;
//	@JsonProperty("fields")
//	private LinkedHashMap<String, FieldDefinition> fieldsMap;

	public TableDefinition() {
		super();
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public List<String> getPkFieldNames() {
		return pkFieldNames;
	}

	public void setPkFieldNames(List<String> pkFieldNames) {
		this.pkFieldNames = pkFieldNames;
	}

	public JoinDefinition getJoinDef() {
		return joinDef;
	}

	public void setJoinDef(JoinDefinition joinDef) {
		this.joinDef = joinDef;
	}
	
	public boolean isJoined() {
		return this.joinDef != null;
	}
//	
//	public FieldDefinition lookupFieldDefinition(String fieldName) {
//		if (fieldsMap == null) {
//			return null;
//		}
//		return fieldsMap.get(fieldName);
//	}
//
//	public FieldDefinition getReferenceField() {
//		if (StringUtils.isEmpty(referenceFieldName)) {
//			return null;
//		}
//		return fieldsMap.get(referenceFieldName);
//	}

	@Override
	public String toString() {
		return "TableDefinition [tableName=" + tableName + ", nickname="
				+ nickname + ", pkFieldNames=" + pkFieldNames + ", joinDef="
				+ joinDef + "]";
	}
}
