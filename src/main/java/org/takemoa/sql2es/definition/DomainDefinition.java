package org.takemoa.sql2es.definition;

import org.springframework.util.StringUtils;
import org.takemoa.sql2es.config.ConfigException;
import org.takemoa.sql2es.sql.SelectBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Describe a domain object
 * @author Take Moa
 */
public class DomainDefinition {

	private LinkedHashMap<String, TypeDefinition> typesMap = null;
	private TypeDefinition rootTypeDef = null;


    // Computed field
    HashMap<String, Object> allFieldsMappings = null;
	
	public DomainDefinition(LinkedHashMap<String, TypeDefinition> typesMap) {
		this.typesMap = typesMap;
		
		init();
	}

	private void init() {
		assert(typesMap != null && !typesMap.isEmpty());
		
		// TODO validate required fields in each type
		
		// Init types
		for (Map.Entry<String, TypeDefinition> entry: typesMap.entrySet()) {
			TypeDefinition typeDefinition = entry.getValue(); 
			typeDefinition.init(entry.getKey());
			String parentKey = typeDefinition.getParentTypeName();
			// Add definition to the parent
			if (parentKey != null) {
				TypeDefinition parentTypeDefinition = typesMap.get(parentKey);
				if (parentTypeDefinition == null) {
					// TODO dedicated exception
					throw new RuntimeException("Missing parent: " + parentKey + " for type:" + entry.getKey());
				}
				parentTypeDefinition.addChildTypeDef(typeDefinition);
			} else {
				if (rootTypeDef != null) {
					// TODO dedicated exception
					throw new RuntimeException("Two top level definitions: " + rootTypeDef.getTypeName() + " and " + typeDefinition.getTypeName());
				}
				rootTypeDef = typeDefinition;
			}
		}
		
		if (rootTypeDef == null) {
			throw new ConfigException("Missing root type");
		}
		
		// TODO check main type required fields

		// Collect field mappings
        allFieldsMappings = new HashMap<String, Object>();
        rootTypeDef.addToFieldsMappings(allFieldsMappings);

        if (allFieldsMappings.isEmpty()) {
            allFieldsMappings = null; // for easier comparison
        }
	}

    public Map<String, Object> getAllFieldsMappings() {
        return allFieldsMappings;
    }

	/**
	 * Merge the values from a single row into the main object (if any) or create a new object
	 * @param rootValues
	 * @param rowValues
	 */
	public Map<String, Object> mergeValues(Map<String, Object> rootValues, Map<String, Map<String, Object>> rowValues) {
		assert(rootTypeDef != null);
		Map<String, Object> rootRowValues = rowValues.get(rootTypeDef.getTypeName());
		if (rootValues == null) {
			rootValues = rootRowValues;
		} else {
			// TODO verify all values are the same
			assert(rootValues.get(rootTypeDef.getIdFieldKey()).equals(rootRowValues.get(rootTypeDef.getIdFieldKey())));
		}
		List<TypeDefinition> childTypeList = rootTypeDef.getChildTypes();
		if (childTypeList == null) {
			return rootValues;
		}
		for (TypeDefinition childType: childTypeList) {
			mergeValuesToParent(rootValues, childType, rowValues);
		}
		
		return rootValues;
	}
	
	private static final String ZERO = "0";
	
	/**
	 * Recursive function to merge the values to the parent
	 * 
	 * @param parentValues
	 * @param childType
	 * @param rowValues
	 */
	@SuppressWarnings("unchecked")
	private void mergeValuesToParent(Map<String, Object> parentValues,
			TypeDefinition childType, Map<String, Map<String, Object>> rowValues) {
		
		// Values that are coming in the RS record
		Map<String, Object> childRowValues = rowValues.get(childType.getTypeName());
		if (childRowValues == null || childRowValues.isEmpty()) {
			return; // nothing to do
		}
		// Check ID is not NULL, or ZERO
		String idFieldKey = childType.getIdFieldKey();
		Object idFieldValue = childRowValues.get(idFieldKey);
		if (idFieldValue == null || idFieldValue.toString().equals(ZERO)) {
			return; // nothing to do again
		}
		
		// Resulting child values
		Map<String, Object> childValues = null;
		
		if (childType.getParentRelation() == MultiplicityEnum.ONE) {
			childValues = (Map<String, Object>)parentValues.get(childType.getParentFieldName());
			if (childValues == null) {
				childValues = childRowValues;
				parentValues.put(childType.getParentFieldName(), childValues);
			} else {
				// TODO verify they have the same ID and/or values
				// nothing to do
			}
		} else { // MultiplicityEnum.MANY
			// Add to list only if not empty
			List<Map<String, Object>> childValuesList = (List<Map<String, Object>>)parentValues.get(childType.getParentFieldName());
			if (childValuesList == null) { // First time
				childValuesList = new ArrayList<Map<String, Object>>();
				childValues = childRowValues;
				childValuesList.add(childValues);
				parentValues.put(childType.getParentFieldName(), childValuesList);
			} else {
				// Check if already in the list
				for (Map<String, Object> childValuesObject: childValuesList) {
					if (idFieldValue.equals(childValuesObject.get(idFieldKey))) {
						childValues = childValuesObject; // already there
						break;
					}
				}
				// Add it only if not already there
				if (childValues == null) {
					childValues = childRowValues;
					childValuesList.add(childValues);
				}
			}
		}
		
		// Process children
		List<TypeDefinition> grandChildTypeList = childType.getChildTypes();
		if (grandChildTypeList != null) {
			for (TypeDefinition grandChildType: grandChildTypeList) {
				mergeValuesToParent(childValues, grandChildType, rowValues);
			}
		}
	}

	/**
	 * @param rowValues
	 * @return The ID of the actual object
	 */
	public String getRootId(Map<String, Map<String, Object>> rowValues) {
		assert(rootTypeDef != null);
		Map<String, Object> rootValues = rowValues.get(rootTypeDef.getTypeName());
		// TODO check it is of type String
		return rootValues.get(rootTypeDef.getIdFieldKey()).toString();
	}
	
	public TypeDefinition getRootTypeDef() {
		return rootTypeDef;
	}

    public String getName() {
        return getRootTypeDef().getTypeName();
    }

	/**
	 * Build a select statement from the table definitions
	 * @param selectBuilder
	 */
	public SelectBuilder buildSelect(SelectBuilder selectBuilder) {
		assert(selectBuilder != null);
		
		for (TypeDefinition typeDef: typesMap.values()) {
			typeDef.addToSelectBuilder(selectBuilder);
		}
		return selectBuilder;
	}
	
	public SelectBuilder addRefFilter(SelectBuilder selectBuilder) {
		// Add only for the root
		rootTypeDef.addRefFilter(selectBuilder);
		return selectBuilder;
	}

    public Collection<TypeDefinition> getAllTypeDefs() {
        return typesMap.values();
    }

    public SelectBuilder addRefSort(SelectBuilder selectBuilder) {
        // Add only for the root
        rootTypeDef.addRefSort(selectBuilder);
        return selectBuilder;
    }

	public FieldDefinition getRefFieldDef() {
		// TODO Auto-generated method stub
		return rootTypeDef.getRefFieldDef();
	}
	
	public FieldType getRefFieldType() {
		
		return rootTypeDef.getRefFieldType();
	}
	
	public Object getRefValue(Map<String, Object> domainObject) {
		if (StringUtils.isEmpty(rootTypeDef.getRefFieldKey())) {
			return null;
		}
		return domainObject.get(rootTypeDef.getRefFieldKey());
	}

	public Map<String, Map<String, Object>> extractRow(ResultSet rs, int rowNum, TimeZone dbServerTimeZone) throws SQLException {
		HashMap<String, Map<String, Object>> rowValues = new HashMap<String, Map<String, Object>>();
		
		for (Map.Entry<String, TypeDefinition> entry: typesMap.entrySet()) {
			Map<String, Object> typeRowValues = entry.getValue().extractRow(rs, rowNum, dbServerTimeZone);
			rowValues.put(entry.getKey(), typeRowValues);
		}
		return rowValues;
	}

	@Override
	public String toString() {
		return "DomainDefinition [typesMap=" + typesMap + ", rootTypeDef="
				+ rootTypeDef + "]";
	}
}
