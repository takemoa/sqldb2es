package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.takemoa.sql2es.sql.SelectBuilder;
import org.takemoa.sql2es.sql.SortTypeEnum;
import org.takemoa.sql2es.util.Conversions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Take Moa
 */
public class TypeDefinition {

/*
<type>:
  # relation to parent object: one object or an array of object; not required for the main domain/object
  inParent: ONE | MANY
  # the field that uniquely identifies the domain
  idField: <field_name_from_field_list>
  # reference field - for sorting and detecting new values
  refField: <field_name_from_field_list>
  # Additional sort fields
  sortFields:
    <field_name_from_field_list>: ASC | DESC
  # List of tables, part of this domain
  tables:
    <table_name_1>:
  fields:
    <field_name_1>:
 */

	// Setup during init
    // e.g. order.parcels.parcelLines
	private String typeName = null;
    // parcelLines
	private String parentFieldName = null;
    // order.parcels
	private String parentTypeName = null;
    // parcels.parcelLines
    private String typePath = null;
	
	private MultiplicityEnum parentRelation = null;
	
	// Field keys
	@JsonProperty("idField")
	private String idFieldKey = null;
	@JsonProperty("refField")
	private String refFieldKey = null;
	@JsonProperty("sortFields")
	private LinkedHashMap<String, SortTypeEnum> sortFields = null;
	@JsonProperty("whereFilters")
	private List<String> whereFilters = null;
	@JsonProperty("tables")
	private LinkedHashMap<String, TableDefinition> tablesMap = null;
	@JsonProperty("fields")
	private LinkedHashMap<String, FieldDefinition> fieldsMap;
    @JsonProperty("updates")
    private LinkedHashMap<String, TypeUpdateDefinition> updatesMap;

	// A list of children type definitions
	private List<TypeDefinition> childTypes = null;
	
	// CONSTANTS
	public final static String SQL_TYPE_SEP = "___";
	public static final String TYPE_NAME_SEP = ".";

	public TypeDefinition() {
	}

	public String getTypeName() {
		return typeName;
	}
	
	public String getParentFieldName() {
		return parentFieldName;
	}

	public String getParentTypeName() {
		return parentTypeName;
	}

    public String getTypePath() {
        return typePath;
    }

    public void addChildTypeDef(TypeDefinition childTypeDefinition) {
		if (childTypes == null) {
			childTypes = new ArrayList<TypeDefinition>();
		}
		childTypes.add(childTypeDefinition);
	}

	public List<TypeDefinition> getChildTypes() {
		return childTypes;
	}

	public MultiplicityEnum getParentRelation() {
		return parentRelation;
	}

	public void setParentRelation(MultiplicityEnum parentRelation) {
		this.parentRelation = parentRelation;
	}

	public String getIdFieldKey() {
		return idFieldKey;
	}

	public void setIdFieldKey(String idFieldKey) {
		this.idFieldKey = idFieldKey;
	}

	public String getRefFieldKey() {
		return refFieldKey;
	}

	public void setRefFieldKey(String refFieldKey) {
		this.refFieldKey = refFieldKey;
	}

	public LinkedHashMap<String, SortTypeEnum> getSortFields() {
		return sortFields;
	}

	public void setSortFields(LinkedHashMap<String, SortTypeEnum> sortFields) {
		this.sortFields = sortFields;
	}
	
	public List<String> getWhereFilters() {
		return whereFilters;
	}

	public void setWhereFilters(List<String> whereFilters) {
		this.whereFilters = whereFilters;
	}

	public LinkedHashMap<String, TableDefinition> getTablesMap() {
		return tablesMap;
	}

	public void setTablesMap(LinkedHashMap<String, TableDefinition> tablesMap) {
		this.tablesMap = tablesMap;
	}

	public LinkedHashMap<String, FieldDefinition> getFieldsMap() {
		return fieldsMap;
	}

	public void setFieldsMap(LinkedHashMap<String, FieldDefinition> fieldsMap) {
		this.fieldsMap = fieldsMap;
	}

    public LinkedHashMap<String, TypeUpdateDefinition> getUpdatesMap() {
        return updatesMap;
    }

    public void setUpdatesMap(LinkedHashMap<String, TypeUpdateDefinition> updatesMap) {
        this.updatesMap = updatesMap;
    }

    private String selectItemPrefix = null;

	/**
	 * Initialize and validate current type
	 * @param typeName
	 */
	public void init(String typeName) {
		this.typeName = typeName;

        // Init parent fields and prefix
        int nameIndex = this.typeName.lastIndexOf(TYPE_NAME_SEP);
        if (nameIndex <= 0) {
            // This is the top level parent
            selectItemPrefix = this.typeName + SQL_TYPE_SEP;
        } else {
            selectItemPrefix = StringUtils.replace(this.typeName, TYPE_NAME_SEP, SQL_TYPE_SEP) + SQL_TYPE_SEP;
            parentTypeName = StringUtils.substring(this.typeName, 0, nameIndex);
            parentFieldName = StringUtils.substring(this.typeName, nameIndex + 1);
            // Get the objectPath
            nameIndex = this.typeName.indexOf(TYPE_NAME_SEP);
            typePath = StringUtils.substring(this.typeName, nameIndex + 1);
        }

		// Set the table name
		if (tablesMap != null) {
			for (Map.Entry<String, TableDefinition> entry: tablesMap.entrySet()) {
				entry.getValue().setTableName(entry.getKey());
			}
		}
		
		if (fieldsMap != null) {
			for (Map.Entry<String, FieldDefinition> fieldEntry: fieldsMap.entrySet()) {
				fieldEntry.getValue().init(fieldEntry.getKey(), typePath);
			}
		}

        if (updatesMap != null) {
            for (Map.Entry<String, TypeUpdateDefinition> entry: updatesMap.entrySet()) {
                FieldDefinition updateRefFieldDef = fieldsMap.get(entry.getValue().getRefField());
                // TODO validate
                assert (updateRefFieldDef != null);
                entry.getValue().init(entry.getKey(), updateRefFieldDef);
            }
        }
		
		// TODO also validate field keys and other fields
		assert(idFieldKey != null);
	}

    /**
     * Add the custom field mappings specific to this type and its subtypes
     * @param parentFieldsMappings the mappings Map to add to
     *
     */
	public void addToFieldsMappings(Map<String, Object> parentFieldsMappings) {
        final String topFieldsKey = "properties";

        Map<String, Object> thisFieldsMappings = new HashMap<String, Object>();

        // Add own custom mappings
        if (fieldsMap != null) {
            for (FieldDefinition fieldDef: fieldsMap.values()) {
               Map<String, String> fieldMappings = fieldDef.getMappings();
               if (fieldMappings != null && !fieldMappings.isEmpty()) {
                   thisFieldsMappings.put(fieldDef.getFieldName(), fieldMappings);
               }
            }
        }

        // Let children add their own field mappings - recursive
        if (childTypes != null) {
            for (TypeDefinition childType : childTypes) {
                Map<String, Object> childFieldsMappings = new HashMap<String, Object>();
                childType.addToFieldsMappings(childFieldsMappings);
                if (!childFieldsMappings.isEmpty()) {
                    thisFieldsMappings.put(childType.getParentFieldName(), childFieldsMappings);
                }
            }
        }

        if (!thisFieldsMappings.isEmpty()) {
            parentFieldsMappings.put(topFieldsKey, thisFieldsMappings);
            if (parentFieldName != null) {
                // Add type=object only for children
                parentFieldsMappings.put("type", "object");
            }
        }
    }
	
	public String fieldAsSelectItem(String fieldName) {
		return selectItemPrefix + fieldName;
	}
	
	public void addToSelectBuilder(SelectBuilder selectBuilder) {
		// 1. Add tables
		if (tablesMap != null) {
			for (Map.Entry<String, TableDefinition> entry: tablesMap.entrySet()) {
				TableDefinition tableDefinition = entry.getValue();
				// FROM/JOIN
				if (tableDefinition.isJoined()) {
					selectBuilder.join(tableDefinition.getTableName(), tableDefinition.getNickname(), tableDefinition.getJoinDef().getJoinString(),
							tableDefinition.getJoinDef().getParentColumns(), tableDefinition.getJoinDef().getChildColumns());
				} else {
					selectBuilder.from(tableDefinition.getTableName(), tableDefinition.getNickname());
				}
			}
		}
		
		// 2. add select fields, and sort by
		if (getFieldsMap() != null) {
			for (FieldDefinition fieldDef: getFieldsMap().values()) {
				String selectItemName = fieldAsSelectItem(fieldDef.getFieldName());
				selectBuilder.select(fieldDef.getSqlExpression(), selectItemName);
				if (sortFields != null) {
					SortTypeEnum sortType = sortFields.get(fieldDef.getFieldName());
					if (sortType != null) {
						selectBuilder.orderBy(selectItemName, sortType);
					}
				}
			}
		}
	
		// 3. Add where
		if (whereFilters != null) {
			for (String whereFilter: whereFilters) {
				selectBuilder.where(whereFilter);
			}
		}
	}

	public FieldDefinition getRefFieldDef() {
		if (StringUtils.isEmpty(refFieldKey)) {
			return null;
		}
		return fieldsMap.get(refFieldKey);
	}

	public FieldType getRefFieldType() {
		FieldDefinition refFieldDef = getRefFieldDef();
		return refFieldDef == null ? null : refFieldDef.getFieldType();
	}

    /**
     * Add filtering by reference value (if refKey is not null)
     * refField >= :last_ref_value
     * @param selectBuilder
     */
    public void addRefFilter(SelectBuilder selectBuilder) {
        // Only root type can add ref filter
        assert (parentFieldName == null);

        if (StringUtils.isNotEmpty(refFieldKey)) {
            FieldDefinition refFieldDef = fieldsMap.get(refFieldKey);
            assert(refFieldDef != null);
            selectBuilder.where(refFieldDef.getSqlExpression() + " >= :" + SelectBuilder.P_REF_VALUE);
        }
    }

    public void addRefSort(SelectBuilder selectBuilder) {
        // Only root type can add ref filter
        assert (parentFieldName == null);
        assert (refFieldKey != null);

        selectBuilder.orderBy(fieldAsSelectItem(refFieldKey), SortTypeEnum.ASC);
    }

    /**
     * Add filtering and sorting fields
     * refField <= :last_ref_value and refUpdateField >= :last_ref_update_value
     * Add additional where clauses
     * Add sort by refUpdateField
     */
    public void addUpdateFiltersAndSort(SelectBuilder selectBuilder,FieldDefinition refFieldDef, TypeUpdateDefinition
            typeUpdateDef, boolean nullRefUpdateValue) {

        // Filters
        selectBuilder.where(refFieldDef.getSqlExpression() + " <= :" + SelectBuilder.P_REF_VALUE);
        if (nullRefUpdateValue) {
            selectBuilder.where(typeUpdateDef.getRefFieldDef().getSqlExpression() + " IS NOT NULL");
        } else {
            selectBuilder.where(typeUpdateDef.getRefFieldDef().getSqlExpression() + " > :" + SelectBuilder
                    .P_REF_UPDATE_VALUE);
        }
        selectBuilder.where(typeUpdateDef.getWhereFilters());

        // Sort by
        selectBuilder.orderByFirst(fieldAsSelectItem(typeUpdateDef.getRefField()), SortTypeEnum.ASC);
    }

    public Map<String, Object> extractRow(ResultSet rs, int rowNum, TimeZone dbServerTimeZone) throws SQLException {
        if (getFieldsMap() == null) {
            return null;
        }

        Map<String, Object> typeRowValues = new LinkedHashMap<String, Object>();
        for (FieldDefinition fieldDef: getFieldsMap().values()) {
            String columnLabel = fieldAsSelectItem(fieldDef.getFieldName());
            Object value = Conversions.sqlToESValue(rs, fieldDef, columnLabel, dbServerTimeZone);
            typeRowValues.put(fieldDef.getFieldName(), value);
        }
        return typeRowValues;
    }

    public boolean hasUpdates() {
        return updatesMap != null && !updatesMap.isEmpty();
    }

    public Collection<TypeUpdateDefinition> getUpdateTypeDefs() {
        if (!hasUpdates()) {
            return null;
        }
        return updatesMap.values();
    }

    @Override
    public String toString() {
        return "TypeDefinition{" +
                "typeName='" + typeName + '\'' +
                ", parentFieldName='" + parentFieldName + '\'' +
                ", parentTypeName='" + parentTypeName + '\'' +
                ", parentRelation=" + parentRelation +
                ", idFieldKey='" + idFieldKey + '\'' +
                ", refFieldKey='" + refFieldKey + '\'' +
                ", sortFields=" + sortFields +
                ", whereFilters=" + whereFilters +
                ", tablesMap=" + tablesMap +
                ", fieldsMap=" + fieldsMap +
                ", updatesMap=" + updatesMap +
                ", childTypes=" + childTypes +
                ", selectItemPrefix='" + selectItemPrefix + '\'' +
                '}';
    }
}
