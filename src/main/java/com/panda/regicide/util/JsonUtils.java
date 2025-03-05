package com.panda.regicide.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String convertObjectToString(Object object) {
        String jsonObject;
        try {
            jsonObject = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Error trying to convert the object into Json");
            throw new RuntimeException(e);
        }
        return jsonObject;
    }
}
