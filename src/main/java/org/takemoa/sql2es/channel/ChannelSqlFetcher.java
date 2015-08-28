package org.takemoa.sql2es.channel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.takemoa.sql2es.definition.ChannelDefinition;
import org.takemoa.sql2es.definition.DomainDefinition;
import org.takemoa.sql2es.definition.TypeUpdateDefinition;
import org.takemoa.sql2es.sql.SelectBuilder;
import org.takemoa.sql2es.util.Conversions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The actual SQL data fetcher.
 *
 * @author Take Moa
 */
public class ChannelSqlFetcher extends RowCountCallbackHandler {

    private static final Logger logger = LogManager.getLogger();

    private ChannelManager channelManager = null;
    private SelectBuilder selectBuilder = null;
    private NamedParameterJdbcTemplate jdbcTemplate = null;
    private final int batchSize;
    private final int maxSize;
    private final TypeUpdateDefinition typeUpdateDefinition;
    Map<String, Object> jdbcParamsMap = new HashMap<String, Object>();

    // Batch record count
    private int batchRecordCount = 0;

    // Internal value map: ID to name-value pairs (JSon object)
    private LinkedHashMap<String, Map<String, Object>> domainObjectMap = new LinkedHashMap<String, Map<String, Object>>();

    private int batchCount = 0;

    public ChannelSqlFetcher(ChannelManager channelManager, SelectBuilder selectBuilder, NamedParameterJdbcTemplate
            jdbcTemplate, int batchSize, int maxSize, TypeUpdateDefinition typeUpdateDefinition) {
        super();
        this.channelManager = channelManager;
        this.selectBuilder = selectBuilder;
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = batchSize;
        this.maxSize = maxSize;
        this.typeUpdateDefinition = typeUpdateDefinition;
        jdbcParamsMap.put(SelectBuilder.P_LIMIT, batchSize);
    }

    public TypeUpdateDefinition getTypeUpdateDefinition() {
        return typeUpdateDefinition;
    }

    /**
     * Bring a new batch of data from database
     */
    public boolean processNewBatch(Object lastReferenceValue, Object lastUpdateReferenceValue) {
        ++batchCount;
        boolean done = false;
        ChannelDefinition channelDefinition = channelManager.getChannelDefinition();
        String channelName = channelManager.getChannelName();
        DomainDefinition domainDefinition = channelManager.getDomainDefinition();

        if (lastReferenceValue != null) {
            jdbcParamsMap.put(SelectBuilder.P_REF_VALUE, lastReferenceValue);
        }

        long startTime = System.currentTimeMillis();
        // a. add conditional placeholder values
        if (lastReferenceValue != null) {
            Object lastRefValueSql = Conversions.localToSqlValue(lastReferenceValue, domainDefinition.getRefFieldDef(), channelDefinition.getDbTimeZone());
            logger.debug("Channel {} batch {} - SQL ref value: {}", channelName, batchCount, lastRefValueSql);
            jdbcParamsMap.put(SelectBuilder.P_REF_VALUE, lastRefValueSql);
        }
        if (lastUpdateReferenceValue != null) {
            // This is for update
            assert (typeUpdateDefinition != null);
            Object lastRefUpdateValueSql = Conversions.localToSqlValue(lastUpdateReferenceValue, domainDefinition
                    .getRefFieldDef(), channelDefinition.getDbTimeZone());
            logger.debug("Channel {} update {} batch {} - SQL update ref value:", channelName, typeUpdateDefinition
                            .getName(),
                    batchCount, lastRefUpdateValueSql);
            jdbcParamsMap
                    .put(SelectBuilder.P_REF_UPDATE_VALUE, lastRefUpdateValueSql);
        }
        // b. Execute the actual query
        this.reset();
        String query = selectBuilder.buildSelectQuery();

        logger.debug("Query:\n{} \nparams:{}", query, jdbcParamsMap);
        jdbcTemplate.query(query, jdbcParamsMap, this);
        // Add reference filter to SQL in preparation for next run
        if (lastReferenceValue == null) {
            domainDefinition.addRefFilter(selectBuilder);
        }
        logger.debug("Channel {}: batch {} took {} ms to bring {} records", channelName, batchCount,
                (System.currentTimeMillis() - startTime), getBatchRecordCount());

        // c. Figure out whether it is done or not
        done = (domainObjectMap.isEmpty() || batchRecordCount < batchSize || (maxSize > 0 && getRowCount() >= maxSize));
        return !done;
    }

    public void reset() {
        batchRecordCount = 0;
    }

    public LinkedHashMap<String, Map<String, Object>> getDomainObjectMap() {
        return domainObjectMap;
    }

    /*
     *
     * @param rs ResultSet to extract data from. This method is
     * invoked for each row
     * @param rowNum number of the current row (starting from 0)
     */
    @Override
    protected void processRow(ResultSet rs, int rowNum) throws SQLException {

        if (batchRecordCount == 0) {
            // prepare for next batch
            domainObjectMap.clear();
        }

        DomainDefinition domainDefinition = channelManager.getDomainDefinition();

        // 1. Extract values -> each type definition has to extract each own values, as HashMap;
        // including the ID
        Map<String, Map<String, Object>> rowValues = domainDefinition.extractRow(rs, rowNum, channelManager.getChannelDefinition().getDbTimeZone());

        // 2. Merge values into the existing map
        String rootId = domainDefinition.getRootId(rowValues);
        Map<String, Object> rootValues = domainObjectMap.get(rootId);
        rootValues = domainDefinition.mergeValues(rootValues, rowValues);
        domainObjectMap.put(rootId, rootValues);

        ++batchRecordCount;
    }

    public int getBatchRecordCount() {
        return batchRecordCount;
    }

    public int getBatchCount() {
        return batchCount;
    }
}
