package org.takemoa.sql2es.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.takemoa.sql2es.definition.ChannelDefinition;
import org.takemoa.sql2es.definition.DomainDefinition;
import org.takemoa.sql2es.definition.FieldType;
import org.takemoa.sql2es.util.Conversions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Config data persisted between channel updates.
 *
 * @author Take Moa
 */
public class ChannelConfigData {

    private String channelName = null;
    private Date lastExecutionDate = null;
    private HashMap<String, Object> lastReference = null;
    private ChannelDefinition channelDef = null;

    public ChannelConfigData() {
    }

    private static ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> toMap() {

        HashMap<String, Object> thisAsMap = mapper.convertValue(this,
                new TypeReference<HashMap<String, Object>>() {
                });
        // Dates are converted to Long by Mapper, change it manually
        thisAsMap.put("lastExecutionDate", lastExecutionDate);
        thisAsMap.put("lastReference", lastReference);

        return thisAsMap;
    }

    /**
     * @param sourceMap
     * @param domainDefinition
     * @return
     */
    public static ChannelConfigData fromMap(Map<String, Object> sourceMap,
                                            DomainDefinition domainDefinition) {

        // Change the types inside the map before conversion, as date fields are
        // returned by ES as String
        Object lastExecutionDate = sourceMap.get("lastExecutionDate");
        sourceMap.put("lastExecutionDate", Conversions.fromEsValue(
                lastExecutionDate, FieldType.DATETIME, null));

        // TODO convert last reference
        ChannelConfigData configData = mapper.convertValue(sourceMap, ChannelConfigData.class);
        FieldType refFieldType = domainDefinition.getRefFieldType();
        if (refFieldType != null) {
            Object refValue = configData.getLastRefValue();
            if (refValue != null) {
                configData.updateLastRefValue(Conversions.fromEsValue(refValue, refFieldType, null));
            }
        }

        return configData;
    }

    public ChannelConfigData(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Date getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(Date lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    @JsonIgnore
    public Object getLastRefValue() {
        if (lastReference == null || lastReference.isEmpty()) {
            return null;
        }
        return lastReference.values().iterator().next();
    }

    public void setLastRefValue(String refName, Object refValue) {
        if (lastReference == null) {
            lastReference = new HashMap<String, Object>();
        }
        lastReference.put(refName, refValue);
    }

    private void updateLastRefValue(Object fieldValue) {
        if (lastReference == null || lastReference.isEmpty()) {
            return;
        }
        lastReference.entrySet().iterator().next().setValue(fieldValue);
    }

    public ChannelDefinition getChannelDef() {
        return channelDef;
    }

    public void setChannelDef(ChannelDefinition channelDef) {
        this.channelDef = channelDef;
    }

    public HashMap<String, Object> getLastReference() {
        return lastReference;
    }

    public void setLastReference(HashMap<String, Object> lastReference) {
        this.lastReference = lastReference;
    }

    @Override
    public String toString() {
        return "ChannelConfigData [channelName=" + channelName
                + ", lastExecutionDate=" + lastExecutionDate
                + ", lastReference=" + lastReference + ", channelDef="
                + channelDef + "]";
    }
}
