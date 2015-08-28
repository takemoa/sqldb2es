package org.takemoa.sql2es.sql;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.HashMap;

/**
 * SQL templates on a per JDBC driver / database type basis
 * 
 * @author Take Moa
 */
public class SqlTemplates {

	// templateSelectClause: "SELECT TOP :limit ${select_list}"
	// templateFromClause: "  FROM ${from_list}"
	// templateWhereClause: "  WHERE ${where_list}"
	// templateOrderByClause: "  ORDER BY ${order_by_list}"
	// templateTableInFromClause: "${table_name} AS ${alias}"
	// templateColumnInSelectList: "${column_name} AS ${alias}"

	@JsonProperty("templateSelectClause")
	private String selectClause = "SELECT TOP :limit ${select_list}";
	@JsonProperty("templateFromClause")
	private String fromClause = "  FROM ${from_list}";
	@JsonProperty("templateWhereClause")
	private String whereClause = "  WHERE ${where_list}";
	@JsonProperty("templateOrderByClause")
	private String orderByClause = "  ORDER BY ${order_by_list}";
	@JsonProperty("templateTableInFromClause")
	private String tableInFromClause = "${table_name} AS ${alias}";
	@JsonProperty("templateColumnInSelectList")
	private String columnInSelectList = "${column_name} AS ${alias}";
	
	public static final SqlTemplates DEFAULT = new SqlTemplates();

	public static final String NL = "\n";
	public static final String INDENT = "     ";
	public static final String COMMA = ", ";
	
	public SqlTemplates() {
		super();
	}
	
	public String getSelectClause() {
		return selectClause;
	}
	public void setSelectClause(String selectClause) {
		this.selectClause = selectClause;
	}
	public String getFromClause() {
		return fromClause;
	}
	public void setFromClause(String fromClause) {
		this.fromClause = fromClause;
	}
	public String getWhereClause() {
		return whereClause;
	}
	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}
	public String getOrderByClause() {
		return orderByClause;
	}
	public void setOrderByClause(String orderByClause) {
		this.orderByClause = orderByClause;
	}
	public String getTableInFromClause() {
		return tableInFromClause;
	}
	public void setTableInFromClause(String tableInFromClause) {
		this.tableInFromClause = tableInFromClause;
	}
	public String getColumnInSelectList() {
		return columnInSelectList;
	}
	public void setColumnInSelectList(String columnInSelectList) {
		this.columnInSelectList = columnInSelectList;
	}

	// Formatting methods
	/**
	 * Creates the select statement formatted according to the internal templates.
	 * 
	 * @param selectList
	 * @param fromList
	 * @param whereList
	 * @param orderByList
	 * @return The SQL select query
	 */
	public String formatSelect(String selectList, String fromList, String whereList, String orderByList) {
		StringBuffer templateBuffer = new StringBuffer(getSelectClause() + NL + getFromClause());
		HashMap<String, String> valuesMap = new HashMap<String, String>(4);
		valuesMap.put("select_list", selectList);
		valuesMap.put("from_list", fromList);
		if (!StringUtils.isEmpty(whereList)) {
			templateBuffer.append(NL + getWhereClause());
			valuesMap.put("where_list", whereList);
		}
		if (orderByList.length() > 0) {
			templateBuffer.append(NL + getOrderByClause());
			valuesMap.put("order_by_list", orderByList.toString());
		}
		
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String selectQuery = sub.replace(templateBuffer);
		
		return selectQuery;
	}

//	@JsonProperty("templateTableInFromClause")
//	private String tableInFromClause = "${table_name} AS ${alias}";

	/**
	 * Format the <table_name> string to be used in from clause of the select statement.
	 * @param tableName
	 * @param asName
	 * @return
	 */
	public Object formatTableNameInFrom(String tableName, String asName) {
		// "${table_name} AS ${alias}";
		HashMap<String, String> valuesMap = new HashMap<String, String>(2);
		valuesMap.put("table_name", tableName);
		valuesMap.put("alias", asName);

		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return sub.replace(tableInFromClause);
	}

//	@JsonProperty("templateColumnInSelectList")
//	private String columnInSelectList = "${column_name} AS ${alias}";
	
	/**
	 * Format the <column_name> string to be used in the select list.
	 * @param tableName
	 * @param asName
	 * @return
	 */
	public Object formatColumnNameInSelectList(String columnName, String asName) {
		// columnInSelectList = "${column_name} AS ${alias}";
		HashMap<String, String> valuesMap = new HashMap<String, String>(2);
		valuesMap.put("column_name", columnName);
		valuesMap.put("alias", asName);

		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return sub.replace(columnInSelectList);
	}

	@Override
	public String toString() {
		return "SqlTemplates [selectClause=" + selectClause + ", fromClause="
				+ fromClause + ", whereClause=" + whereClause
				+ ", orderByClause=" + orderByClause + ", tableInFromClause="
				+ tableInFromClause + ", columnInSelectList="
				+ columnInSelectList + "]";
	}
}
