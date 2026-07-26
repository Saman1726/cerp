package io.cerp.identity.converter;

import io.cerp.identity.domain.ResourceMembership.ResourceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for mapping ResourceType enum to/from database VARCHAR.
 */
@Converter(autoApply = true)
public class ResourceTypeConverter implements AttributeConverter<ResourceType, String> {

    @Override
    public String convertToDatabaseColumn(ResourceType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ResourceType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return ResourceType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ResourceType value: " + dbData, e);
        }
    }
}
