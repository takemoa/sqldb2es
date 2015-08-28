package org.takemoa.sql2es.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.takemoa.sql2es.config.ChannelConfigData;
import org.takemoa.sql2es.config.ConfigManager;
import org.takemoa.sql2es.definition.*;
import org.takemoa.sql2es.es.ESClientManager;
import org.takemoa.sql2es.sql.SelectBuilder;
import org.takemoa.sql2es.sql.SqlTemplates;
import org.takemoa.sql2es.util.Conversions;

import java.util.*;

/**
 * Manages a single channel data into Elastic Search
 *
 * @author Take Moa
 */
public class ChannelManager {
    private static final Logger logger = LogManager.getLogger();

    // Automatic field to be added to any domain object
    public static final String FIELD_CHANNEL = "channel_";
    private static ObjectMapper mapper = new ObjectMapper();
    private String channelName;
    private ConfigManager configManager;
    private ChannelDefinition channelDefinition;
    private DomainDefinition domainDefinition;
    // Calculated/initialized values
    private int batchSize;
    private int maxRecords;
    private String esClusterName = null;

    public ChannelManager(String channelName, ConfigManager configManager,
                          ChannelDefinition channelDefinition,
                          DomainDefinition domainDefinition) {
        super();
        this.channelName = channelName;
        this.configManager = configManager;
        this.channelDefinition = channelDefinition;
        this.domainDefinition = domainDefinition;

        init();
    }

    private void init() {
        batchSize = channelDefinition.getBatchSize();
        if (batchSize <= 0) {
            batchSize = configManager.getDefaultBatchSize();
        }
        maxRecords = channelDefinition.getMaxRecords();
        esClusterName = channelDefinition.getEsClusterName();
        assert (StringUtils.isNotEmpty(esClusterName));
    }

    public String getChannelName() {
        return channelName;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChannelDefinition getChannelDefinition() {
        return channelDefinition;
    }

    public DomainDefinition getDomainDefinition() {
        return domainDefinition;
    }

    /**
     * Does the actual processing, load from database, adding to
     * ElasticSearch.
     */
    public void execute() {

        // Init variables
        // JDBC DATA SOURCE
        DatasourceDefinition dataSourceDef = configManager.getDatasourceDef(channelDefinition.getDatasourceName());
        // SQL TEMPLATES (per jdbc driver)
        SqlTemplates sqlTemplates = configManager.getSqlTemplates(dataSourceDef.getJdbcDriverClassName());
        // Previously persisted config data
        ChannelConfigData configData = lookupConfigData();
        // Reference field
        FieldDefinition refFieldDef = domainDefinition.getRefFieldDef();
        // Retrieve lastReferenceValue from ES index directly
        Object lastReferenceValue = null;
        boolean veryFirstTime = false;
        if (configData == null) {
            configData = new ChannelConfigData(this.channelName);
            veryFirstTime = true;
        } else {
            lastReferenceValue = getChannelMaxReferenceValue();
        }
        // TODO Compare domain and channel def???
        configData.setChannelDef(channelDefinition);

        // TODO remove - this is for test only
        // setConfigDataMapping();

        // Apply custom field mappings (if any)
        setCustomFieldsMappings();

        // 1. Create and initialize the select builder and row handler - only
        // first time
        SelectBuilder mainSelectBuilder = domainDefinition.buildSelect(new SelectBuilder(sqlTemplates));
        // JDBC template and parameters
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(
                dataSourceDef.getDataSource());
        ArrayList<ChannelSqlFetcher> updateSqlFetchers = new ArrayList<ChannelSqlFetcher>();
        // Save a reference select builder
        SelectBuilder refSelectBuilder = mainSelectBuilder.clone();

        domainDefinition.addRefSort(mainSelectBuilder);
        if (lastReferenceValue != null) {
            domainDefinition.addRefFilter(mainSelectBuilder);
        }

        boolean hasMore = true;

        ChannelSqlFetcher newDataSqlFetcher = new ChannelSqlFetcher(this, mainSelectBuilder, jdbcTemplate, batchSize,
                maxRecords, null);

        while (hasMore)  {
            // Execute first the updates so as to update the previous new batches
            if (lastReferenceValue != null) {
                for (TypeDefinition typeDef: domainDefinition.getAllTypeDefs()) {
                    if (typeDef.hasUpdates()) {
                        for (TypeUpdateDefinition typeUpdateDef: typeDef.getUpdateTypeDefs()) {
                            Object lastUpdateReferenceValue = getChannelMaxFieldValue(typeUpdateDef.getRefFieldDef());
                            ChannelSqlFetcher updateSqlFetcher = createUpdateFetcher(refSelectBuilder, typeDef,
                                    typeUpdateDef,
                                    lastUpdateReferenceValue, jdbcTemplate);

                            updateSqlFetcher.processNewBatch(lastReferenceValue, lastUpdateReferenceValue);

                            LinkedHashMap<String, Map<String, Object>> updateDomainObjectMap = updateSqlFetcher
                                    .getDomainObjectMap();

                            // TODO dedicated update method
                            if (!updateDomainObjectMap.isEmpty()) {
                                Object updateLastReferenceValue = storeValues(updateDomainObjectMap, updateSqlFetcher
                                        .getBatchCount(), true);
                            }
                        }
                    }
                }

            }

            // Check new fields
            hasMore = newDataSqlFetcher.processNewBatch(lastReferenceValue, null);

            LinkedHashMap<String, Map<String, Object>> domainObjectMap = newDataSqlFetcher
                    .getDomainObjectMap();
            if (!domainObjectMap.isEmpty()) {
                // Store domain object into ES
                lastReferenceValue = storeValues(domainObjectMap, newDataSqlFetcher.getBatchCount(),
                        !hasMore);
                // TODO { replace with audit data
                if (refFieldDef != null) {
                    configData.setLastRefValue(refFieldDef.getFieldName(),
                            lastReferenceValue);
                }
                configData.setLastExecutionDate(new Date());
                persistConfigData(configData, veryFirstTime);
                // } replace with audit data
                veryFirstTime = false;
            }
        }

        // TODO Another update here

    }

    /**
     * Create a new update sql fetcher. The select statement depends on whether the lastUpdateReferenceValue is null
     * or not
     * @param refSelectBuilder
     * @param typeUpdateDef
     * @param lastUpdateReferenceValue
     * @return
     */
    private ChannelSqlFetcher createUpdateFetcher(SelectBuilder refSelectBuilder, TypeDefinition typeDefinition,
                                                  TypeUpdateDefinition typeUpdateDef,
                                                  Object lastUpdateReferenceValue, NamedParameterJdbcTemplate
                                                          jdbcTemplate) {
        SelectBuilder updateSelectBuilder = refSelectBuilder.clone();
        typeDefinition.addUpdateFiltersAndSort(updateSelectBuilder, domainDefinition.getRefFieldDef(), typeUpdateDef,
                (lastUpdateReferenceValue == null));
        return new ChannelSqlFetcher(this, updateSelectBuilder, jdbcTemplate, Integer.MAX_VALUE,
                -1, typeUpdateDef);
    }

    /**
     * Add the custom ES fields mappings, such as not_analyzed. It does succeed only for fields that do not have any existing (default or not) mappings
     */
    private void setCustomFieldsMappings() {
        if (domainDefinition.getAllFieldsMappings() == null) {
            return; // nothing to do
        }

        Client esClient = ESClientManager.get(esClusterName);
        // Try to create index first - in case this is the first call
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(channelDefinition.getEsIndex());
        try {
            esClient.admin().indices().create(createIndexRequest).actionGet();
        } catch (IndexAlreadyExistsException iaee) {
            // Ignore
        }

        PutMappingRequest putMappingRequest = new PutMappingRequest(/*index*/ channelDefinition.getEsIndex());
        putMappingRequest.type(channelDefinition.getEsType());
        putMappingRequest.source(domainDefinition.getAllFieldsMappings());
        putMappingRequest.ignoreConflicts(true);

        esClient.admin().indices().putMapping(putMappingRequest).actionGet();
    }

    /**
     * Lookup in the existing index the maximum value of the reference field
     *
     * @return max value or null if there is no reference field
     */
    private Object getChannelMaxReferenceValue() {
        FieldDefinition refFieldDef = domainDefinition.getRefFieldDef();
        if (refFieldDef == null) {
            // No reference field, return null
            return null;
        }
        return getChannelMaxFieldValue(refFieldDef);
    }

    private Object getChannelMaxFieldValue(FieldDefinition fieldDef) {
        if (fieldDef == null) {
            // No reference field, return null
            return null;
        }
        Client esClient = ESClientManager.get(esClusterName);
        // Create search request
        final String maxRef = "maxRef";

        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(/*index*/ channelDefinition.getEsIndex());
        searchRequestBuilder.setTypes(channelDefinition.getEsType());
        // REPLACE WITH TERM
        searchRequestBuilder.setQuery(QueryBuilders.matchQuery(ChannelManager.FIELD_CHANNEL, channelDefinition.getName()));
        searchRequestBuilder.addAggregation(AggregationBuilders.max(maxRef).field(
                fieldDef.getFieldPath()));
        searchRequestBuilder.setSize(0);

        SearchResponse searchResponse = null;

        try {
            searchResponse = searchRequestBuilder.execute().actionGet();
        } catch (IndexMissingException ime) {
            return null; // not created yet
        } catch (RuntimeException re) {
            throw re;
        }

        logger.debug("Total records before this batch: {}", searchResponse.getHits().totalHits());

        if (searchResponse.getHits().totalHits() == 0) {
            return null; // first time
        }
        Max maxAgg = searchResponse.getAggregations().get(maxRef);
        Object maxValue = null;
        double maxAggValue = maxAgg.getValue();
        if (maxAggValue != Double.NEGATIVE_INFINITY && maxAggValue != Double.POSITIVE_INFINITY) {
            maxValue = Conversions.fromEsValue(maxAgg.getValue(), fieldDef.getFieldType(), null);
        }

        // How to cast to a date
        logger.debug("Max {} value before this batch:{} ---> {}", fieldDef.getFieldPath(), maxAgg.getValue(), maxValue);

        return maxValue;

    }

    /**
     * Persist values into the ES cluster
     *
     * @param domainObjectMap the values to be stored
     * @param lastBatch       whether this is the last batch
     * @return
     */
    private Object storeValues(
            LinkedHashMap<String, Map<String, Object>> domainObjectMap,
            int batchCount, boolean lastBatch) {
        // If not the last batch, do not insert the last domain object
        // as it could
        // be incomplete. Instead make sure it will be part of the next
        // batch.
        Object lastRefValue = null;
        Map.Entry<String, Map<String, Object>> lastEntry = null;
        Iterator<Map.Entry<String, Map<String, Object>>> it = domainObjectMap
                .entrySet().iterator();
        while (it.hasNext()) {
            lastEntry = it.next();
            // Set the unconditional 'channel' field
            lastEntry.getValue().put(FIELD_CHANNEL, channelName);
        }
        lastRefValue = domainDefinition.getRefValue(lastEntry.getValue());

        if (!lastBatch) {
            // Remove the last value from map as the next value will be picked
            // up anyways
            domainObjectMap.remove(lastEntry.getKey());
        }

        long startTime = System.currentTimeMillis();
        // Do persist into ES here
        Client esClient = ESClientManager.get(esClusterName);
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        // either use client#prepare, or use Requests# to directly build
        // index/delete requests
        for (Map.Entry<String, Map<String, Object>> entry : domainObjectMap
                .entrySet()) {
            bulkRequest.add(esClient.prepareIndex(
                    channelDefinition.getEsIndex(),
                    channelDefinition.getEsType(), entry.getKey()).setSource(
                    entry.getValue()));
        }

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();

        logger.debug("Channel {} batch {} took {} ms to insert {} domain objects; last ref={} last ID={}", channelName,
                batchCount, (System.currentTimeMillis() - startTime), domainObjectMap.size(), lastRefValue, lastEntry.getKey());

        if (bulkResponse.hasFailures()) {
            // process failures by iterating through each bulk response item
            logger.debug("Bulk response has failures:\n{}", bulkResponse.buildFailureMessage());
        }

        // Update the ES Channel values (in ES)

        return lastRefValue;
    }

    /**
     * @return previously persisted channel config data.
     */
    private ChannelConfigData lookupConfigData() {
        Client esClient = ESClientManager.get(esClusterName);
        GetRequestBuilder getRequestBuilder = esClient.prepareGet(
                ConfigManager.ES_CONFIG_INDEX, ConfigManager.ES_CONFIG_TYPE,
                channelName);

        GetResponse getResponse = null;

        try {
            getResponse = getRequestBuilder.get();
        } catch (IndexMissingException ime) {
            return null; // not created yet
        }
        if (!getResponse.isExists()) {
            return null;
        }

        // Convert ...
        ChannelConfigData configData = ChannelConfigData.fromMap(getResponse
                .getSource(), domainDefinition);

        return configData;
    }

    private void setConfigDataMapping() {
        // TODO Why is it not working??
        // String refEsType = "null";
        // FieldType refFieldType = domainDefinition.getRefFieldType();
        // if (refFieldType != null) {
        // refEsType = refFieldType.toEsType();
        // }
        //
        // Client esClient = ESClientManager.get(esClusterName);
        // try {
        // // TODO is it necessary?
        // esClient.admin()
        // .indices()
        // .create(new CreateIndexRequest(
        // ConfigManager.ES_CONFIG_INDEX)).actionGet();
        // } catch (IndexAlreadyExistsException iaee) {
        // // just ignore
        // }
        //
        // try {
        // XContentBuilder xcontentBuilder = XContentFactory.jsonBuilder()
        // .startObject().startObject(channelName)
        // .startObject("properties").startObject("lastExecutionTime")
        // .field("type", "date").endObject()
        // .startObject("lastRefValue").field("type", refEsType)
        // .endObject().endObject().endObject().endObject();
        //
        // PutMappingResponse putMappingResponse = esClient
        // .admin().indices()
        // .preparePutMapping(ConfigManager.ES_CONFIG_INDEX)
        // .setType(channelName)
        // .setSource(xcontentBuilder)
        // .execute().actionGet();
        // } catch (IOException e) {
        // // TODO handle exception
        // System.err.println(e);
        // }
    }

    /**
     * Persist the current config data for future use.
     */
    private void persistConfigData(ChannelConfigData configData,
                                   boolean updateMapping) {
        // First set the mapping
        if (updateMapping) {
            setConfigDataMapping();
        }

        // Then update data
        Client esClient = ESClientManager.get(esClusterName);
        IndexRequestBuilder indexRequestBuilder = esClient.prepareIndex(
                ConfigManager.ES_CONFIG_INDEX, ConfigManager.ES_CONFIG_TYPE,
                channelName);

        indexRequestBuilder.setSource(configData.toMap());

        IndexResponse indexResponse = indexRequestBuilder.execute().actionGet();
        logger.debug("Config data created (updated=false): {}",indexResponse.isCreated());
    }

    @Override
    public String toString() {
        return "ChannelManager [channelName=" + channelName
                + ", channelDefinition=" + channelDefinition
                + ", domainDefinition=" + domainDefinition + "]";
    }
}
