package org.takemoa.sql2es.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Created on 2015-07-03.
 *
 * @author catalin
 */
public class TypeUpdateDefinition {
/*
    inbundParcels.new:
        refField: placedDate
        # The max filters automatically added: cpOrder.CpPlacedDate <= :MAX(localPlacedDate) && cpInboundParcel
        # .PlacedDate >= :MAX(inboundParcel.PlacedDate)
        # MAX(localConfirmedDate)
        whereFilters:
                - "cpInboundParcel.PlacedDate > cpOrder.CpPlacedDate"
*/
    private String refField = null;
    private List<String> whereFilters = null;

    @JsonIgnore
    private String name = null;
    @JsonIgnore
    private FieldDefinition refFieldDef = null;


    public TypeUpdateDefinition() {
    }

    public void init(String name, FieldDefinition refFieldDef) {
        this.name = name;
        this.refFieldDef = refFieldDef;
    }


    public String getName() {
        return name;
    }

    public FieldDefinition getRefFieldDef() {
        return refFieldDef;
    }
    public String getRefField() {
        return refField;
    }

    public void setRefField(String refField) {
        this.refField = refField;
    }

    public List<String> getWhereFilters() {
        return whereFilters;
    }

    public void setWhereFilters(List<String> whereFilters) {
        this.whereFilters = whereFilters;
    }

    @Override
    public String toString() {
        return "TypeUpdateDefinition{" +
                "refField='" + refField + '\'' +
                ", whereFilters=" + whereFilters +
                '}';
    }
}
