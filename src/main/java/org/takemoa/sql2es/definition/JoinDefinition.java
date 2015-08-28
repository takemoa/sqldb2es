package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Take Moa
 */
public class JoinDefinition {

/*
  join:
    type: INNER | LEFT_OUTER
    parentColumns:
    - <parentTableName>.<columns1>
    childColumns:
    - <childTableName>.<columns2>
*/

	@JsonProperty("type")
	private JoinTypeEnum joinType; // todo enum

	@JsonProperty("parentColumns")
	private List<String> parentColumns;
	@JsonProperty("childColumns")
	private List<String> childColumns;

	public JoinDefinition() {
		super();
	}

	public JoinTypeEnum getJoinType() {
		return joinType;
	}

	public void setJoinType(JoinTypeEnum joinType) {
		this.joinType = joinType;
	}

	public List<String> getParentColumns() {
		return parentColumns;
	}

	public void setParentColumns(List<String> parentColumns) {
		this.parentColumns = parentColumns;
	}

	public List<String> getChildColumns() {
		return childColumns;
	}

	public void setChildColumns(List<String> childColumns) {
		this.childColumns = childColumns;
	}

	public String getJoinString() {
		switch (joinType) {
		case INNER:
			return "INNER JOIN";
		case LEFT_OUTER:
			return "LEFT OUTER JOIN";
		}
		return null;
	}

	@Override
	public String toString() {
		return "JoinDefinition [joinType=" + joinType + ", parentColumns="
				+ parentColumns + ", childColumns=" + childColumns + "]";
	}
}
