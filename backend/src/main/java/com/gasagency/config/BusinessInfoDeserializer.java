package com.gasagency.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gasagency.entity.BusinessInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom deserializer to handle BusinessInfo deserialization from ID or object.
 * Allows frontend to send either businessId: 1 or full BusinessInfo object.
 */
@Component
public class BusinessInfoDeserializer extends JsonDeserializer<BusinessInfo> {

    @Override
    public BusinessInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Handle numeric ID (businessId: 1)
        if (p.currentToken().isNumeric()) {
            Long id = p.getLongValue();
            BusinessInfo business = new BusinessInfo();
            business.setId(id);
            return business;
        }

        // Handle null
        if (p.currentToken() == null || p.currentToken().name().equals("NULL")) {
            return null;
        }

        // Handle object - deserialize fully
        return ctxt.readValue(p, BusinessInfo.class);
    }
}

