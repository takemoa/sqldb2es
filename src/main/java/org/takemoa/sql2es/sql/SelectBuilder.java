package org.takemoa.sql2es.sql;

import java.util.List;

/**
 * Helper for creating a select statement compatible with JDBC
 * 
 * @author Take Moa
 */
public class SelectBuilder {

	// Placeholder NAMES
	// TODO Move them to a different place
	public static final String P_LIMIT = "limit";
	public static final String P_REF_VALUE = "last_ref_value";
    public static final String P_REF_UPDATE_VALUE = "last_upd_ref_value";

    // SQL templates to be used wen generating the SELECT statement
	private SqlTemplates sqltemplates = null;

	// Builders
	private StringBuffer selectListBuilder = new StringBuffer();
	private StringBuffer fromListBuilder = new StringBuffer();
	private StringBuffer whereListBuilder = new StringBuffer();
	private StringBuffer orderByBuilder = new StringBuffer();

	public SelectBuilder(SqlTemplates sqltemplates) {
		this.sqltemplates = sqltemplates;
	}

    /**
     * Create a copy of this SelectBuilder
     * @return
     */
    public SelectBuilder clone() {
        SelectBuilder newBuilder = new SelectBuilder(this.sqltemplates);
        newBuilder.selectListBuilder.append(this.selectListBuilder.toString());
        newBuilder.fromListBuilder.append(this.fromListBuilder.toString());
        newBuilder.whereListBuilder.append(this.whereListBuilder.toString());
        newBuilder.orderByBuilder.append(this.orderByBuilder.toString());

        return newBuilder;
    }

	public SelectBuilder select(String columnExpression, String asName) {
		if (selectListBuilder.length() > 0) {
			selectListBuilder.append(SqlTemplates.NL);
			selectListBuilder.append(SqlTemplates.INDENT).append(
					SqlTemplates.COMMA);
		}
		selectListBuilder.append(columnExpression);
		if (asName != null) {
			selectListBuilder.append(" AS " + asName);
		}
		return this;
	}

	public SelectBuilder from(String tableName, String asName) {
		if (fromListBuilder.length() > 0) {
			fromListBuilder.append(SqlTemplates.NL).append(SqlTemplates.COMMA);
		}
		// fromListBuilder.append(tableName + " AS " + asName);
		fromListBuilder.append(sqltemplates.formatTableNameInFrom(tableName,
				asName));
		return this;
	}

	public SelectBuilder join(String tableName, String asName,
			String joinString, List<String> parentKeys, List<String> childKeys) {
		// TODO Use template
		// TODO joinString == constant (or enum)
		// INNER JOIN dbo.cpOrderConsignee AS cpoc WITH (NOLOCK) ON
		// cpod.OrderConsigneeId = cpoc.OrderConsigneeId
		assert (fromListBuilder.length() > 0);
		assert (parentKeys.size() == childKeys.size());
		assert (parentKeys.size() > 0);
		fromListBuilder.append(SqlTemplates.NL).append(SqlTemplates.INDENT)
				.append(joinString).append(" ");
		fromListBuilder.append(sqltemplates.formatTableNameInFrom(tableName,
				asName));
		fromListBuilder.append(" ON " + parentKeys.get(0) + " = "
				+ childKeys.get(0));
		for (int i = 1; i < parentKeys.size(); ++i) {
			fromListBuilder.append(" AND " + parentKeys.get(i) + " = "
					+ childKeys.get(i));
		}

		return this;
	}

	public SelectBuilder where(String whereCondition) {
		if (whereListBuilder.length() > 0) {
			whereListBuilder.append(SqlTemplates.NL)
					.append(SqlTemplates.INDENT).append("AND ");
		}
		whereListBuilder.append(whereCondition);
		return this;
	}

    public SelectBuilder where(List<String> whereConditionList) {
        if (whereConditionList != null) {
            for (String whereCondition: whereConditionList) {
                this.where(whereCondition);
            }
        }
        return this;
    }

	public SelectBuilder orderBy(String orderByField, SortTypeEnum sortType) {
		if (orderByBuilder.length() > 0) {
			orderByBuilder.append(SqlTemplates.NL).append(SqlTemplates.INDENT)
					.append(SqlTemplates.COMMA);
		}
		orderByBuilder.append(orderByField);
		if (sortType == SortTypeEnum.ASC) {
			orderByBuilder.append(" ASC");
		} else {
			orderByBuilder.append(" DESC");
		}
		return this;
	}

    /**
     * Add to the order by as the first
     * @param orderByField
     * @param sortType
     * @return
     */
    public SelectBuilder orderByFirst(String orderByField, SortTypeEnum sortType) {
        if (orderByBuilder.length() > 0) {
            orderByBuilder.insert(0, SqlTemplates.NL + SqlTemplates.INDENT + SqlTemplates.COMMA);
        }
        if (sortType == SortTypeEnum.ASC) {
            orderByBuilder.insert(0, orderByField + " ASC");
        } else {
            orderByBuilder.insert(0, orderByField + " DESC");
        }
        return this;
    }


    public String buildSelectQuery() {
		// Use template commons substr
		// private String templateSelectClause =
		// "SELECT TOP :limit ${select_list}";
		// private String templateFromClause = "   FROM ${from_list}\n";
		// private String templateWhereClause = "  WHERE ${where_list}\n";
		// private String templateOrderByClause =
		// "  ORDER BY ${order_by_list}\n";

		return sqltemplates.formatSelect(selectListBuilder.toString(),
				fromListBuilder.toString(), whereListBuilder.toString(),
				orderByBuilder.toString());
	}
}
