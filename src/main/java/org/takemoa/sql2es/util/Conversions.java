package org.takemoa.sql2es.util;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.takemoa.sql2es.definition.FieldDefinition;
import org.takemoa.sql2es.definition.FieldType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Data conversion between various types
 * @author Take Moa
 */
public class Conversions {

	/**
	 * Convert from a value as received from ES API to a value having the expected type as per fieldType.
	 * 
	 * @param esValue
	 * @param fieldType
	 * @return
	 */
	public static Object fromEsValue(Object esValue, FieldType fieldType) {
		if (esValue == null) {
			return null;
		}
		
		if (fieldType == FieldType.DATE || fieldType == FieldType.DATETIME) {
			// Must return a date
			Class<?> esClass = esValue.getClass();
			if (esClass == Date.class) {
				return esValue;
			} else if (esClass == String.class) {
				// convert from String using Joda
                return XContentBuilder.defaultDatePrinter.parseDateTime((String)esValue).toDate();
			} else if (esClass == Long.class) {
				// Create one automatically - Use UTC (as dates are coming from ES as UTC)
                Timestamp dateTimeUtc = new Timestamp(((Long)esValue).longValue());
                return dateTimeUtc;
            } else if (esClass == Double.class) {
                // Dates are coming as UTC
                Timestamp dateTimeUtc = new Timestamp(((Double)esValue).longValue());
                return dateTimeUtc;
//                // Aggregate values are coming as Double from ES.
//                DateTime dateTimeUtc = new DateTime(((Double)esValue).longValue(), DateTimeZone.UTC );
//                Date date = dateTimeUtc.toDate();
//                return date;
			}
		// TODO implement the other types also
//        if (refFieldDef.getFieldType() == FieldType.DATE) {
//            maxValue = new java.sql.Date((long) esValue);
//        } else if (refFieldDef.getFieldType() == FieldType.DATETIME) {
//            maxValue = new java.sql.Timestamp((long) esValue);
//        } else if (fieldType == FieldType.FLOAT) {
//            maxValue = new Float(esValue);
//        } else if (fieldType == FieldType.DOUBLE) {
//            maxValue = new Double(esValue);
//        } else if (fieldType == FieldType.INTEGER) {
//            maxValue = new Integer((int) esValue);
//        } else if (fieldType == FieldType.LONG) {
//            maxValue = new Long((long) esValue);
//        } else if (fieldType == FieldType.SHORT) {
//            maxValue = new Short((short) esValue);
        }
		
		return esValue;
	}

    /**
     * Convert a value received from DB to the corresponding ES field value.
     *
     * @param rs
     * @param fieldDef
     * @param columnLabel
     * @param dbTimeZone
     * @return The ES field value
     * @throws SQLException
     */
    public static Object sqlToESValue(ResultSet rs, FieldDefinition fieldDef, String columnLabel, TimeZone dbTimeZone) throws SQLException {

        switch (fieldDef.getFieldType()) {
            case STRING:
                return rs.getString(columnLabel);
            case DATE:
                return rs.getDate(columnLabel);
            case DATETIME:
                // TODO - get the timezone from configuration
                if (dbTimeZone == null) {
                    return rs.getTimestamp(columnLabel);
                }
                return rs.getTimestamp(columnLabel, Calendar.getInstance(dbTimeZone));
            case FLOAT:
                return rs.getFloat(columnLabel);
            case DOUBLE:
                return rs.getDouble(columnLabel);
            case INTEGER:
                return rs.getInt(columnLabel);
            case LONG:
                return rs.getLong(columnLabel);
            case SHORT:
                return rs.getShort(columnLabel);
            case BOOLEAN:
                return rs.getBoolean(columnLabel);
        }
        return null;

    };

    /**
     * Convert a local field (usually a timestamp) from a local format (e.g. local time zone) to the db format (e.g. DB time zone)
     * @param fieldDef
     * @param dbTimeZone
     * @return
     */
    public static Object localToSqlValue(Object value, FieldDefinition fieldDef, TimeZone dbTimeZone) {
        // Currently only applies to timestamps if the time zone is not empty
        if (value == null || fieldDef.getFieldType() != FieldType.DATETIME || dbTimeZone == null) {
            return value;
        }
        Class<?> valueClass = value.getClass();

        if (Date.class.isAssignableFrom(valueClass)) {
            // TODO is there any way to use Gregorian???
            // Convert a value from local timezone to the db time zone
            DateTime dateTime = new DateTime((Date) value, DateTimeZone.forTimeZone(dbTimeZone));
            dateTime = dateTime.withZoneRetainFields(DateTimeZone.getDefault());

            // Convert from one timezone to another
            if (valueClass == Timestamp.class) {
                return new Timestamp(dateTime.getMillis());
            }
            return dateTime.toDate();
        }

        return value;
    };
}
