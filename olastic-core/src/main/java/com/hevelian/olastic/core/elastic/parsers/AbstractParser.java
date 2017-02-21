package com.hevelian.olastic.core.elastic.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;

import com.hevelian.olastic.core.edm.ElasticEdmComplexType;
import com.hevelian.olastic.core.edm.ElasticEdmEntityType;

/**
 * Abstract parser with common behavior for all parsers.
 * 
 * @author rdidyk
 *
 * @param <T>
 *            instance data type class
 * @param <V>
 *            instance data value class
 */
public abstract class AbstractParser<T, V> implements ESResponseParser<T, V> {

    @SuppressWarnings("unchecked")
    protected Property createProperty(String name, Object value, ElasticEdmEntityType entityType) {
        EdmElement property = entityType.getProperty(name);
        if (value instanceof List) {
            return createPropertyList(name, (List<Object>) value, entityType);
        } else if (value instanceof Map) {
            return createComplexProperty(name, (Map<String, Object>) value,
                    entityType.getProperty(name));
        } else if (property != null) {
            Object modifiedValue = value;
            if (property.getType() instanceof EdmDate
                    || property.getType() instanceof EdmDateTimeOffset) {
                if (value != null) {
                    modifiedValue = DatatypeConverter.parseDateTime((String) value).getTime();
                }
            } else if (property.getType() instanceof EdmBoolean && value instanceof Long) {
                // When Elasticsearch aggregates data it return's boolean as
                // number value (1,0), but when it searches then normal boolean
                // value will be retrieved
                modifiedValue = (Long) value != 0;
            }
            return createPrimitiveProperty(name, modifiedValue);
        } else {
            return createPrimitiveProperty(name, value);
        }
    }

    private Property createPrimitiveProperty(String name, Object value) {
        return new Property(null, name, ValueType.PRIMITIVE, value);
    }

    private Property createComplexProperty(String name, Map<String, Object> value,
            EdmElement edmElement) {
        ComplexValue complexValue = createComplexValue(value, edmElement);
        return new Property(null, name, ValueType.COMPLEX, complexValue);
    }

    @SuppressWarnings("unchecked")
    private Property createPropertyList(String name, List<Object> valueObject,
            EdmStructuredType structuredType) {
        ValueType valueType;
        EdmElement property = structuredType.getProperty(name);
        EdmTypeKind propertyKind = property.getType().getKind();
        if (propertyKind == EdmTypeKind.COMPLEX) {
            valueType = ValueType.COLLECTION_COMPLEX;
        } else {
            valueType = ValueType.COLLECTION_PRIMITIVE;
        }
        List<Object> properties = new ArrayList<>();
        for (Object value : valueObject) {
            if (value instanceof Map) {
                properties.add(createComplexValue((Map<String, Object>) value, property));
            } else {
                properties.add(value);
            }
        }
        return new Property(null, name, valueType, properties);
    }

    @SuppressWarnings("unchecked")
    private ComplexValue createComplexValue(Map<String, Object> complexObject,
            EdmElement edmElement) {
        ComplexValue complexValue = new ComplexValue();
        for (Map.Entry<String, Object> entry : complexObject.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List) {
                ElasticEdmComplexType complexType = (ElasticEdmComplexType) edmElement.getType();
                EdmElement complexProperty = complexType.getProperty(entry.getKey());
                complexProperty = complexProperty == null
                        ? complexType.getPropertyByNestedName(entry.getKey()) : complexProperty;
                complexValue.getValue().add(createPropertyList(complexProperty.getName(),
                        (List<Object>) value, complexType));
            } else {
                complexValue.getValue().add(
                        new Property(null, entry.getKey(), ValueType.PRIMITIVE, entry.getValue()));
            }
        }
        return complexValue;
    }
}
