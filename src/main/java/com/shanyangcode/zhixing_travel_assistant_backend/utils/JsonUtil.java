package com.shanyangcode.zhixing_travel_assistant_backend.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonUtil {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    static {
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    public static <T> T toObj(String str, Class<T> clz) {
        try {
            return jsonMapper.readValue(str, clz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> T toObj(Map<Object, Object> map, Class<T> clz) {
        return jsonMapper.convertValue(map, clz);
    }

    public static <T> T toObj(String str, TypeReference<T> clz) {
        try {
            return jsonMapper.readValue(str, clz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> List<T> toList(String str, Class<T> clz) {
        try {
            return jsonMapper.readValue(str, new TypeReference<List<T>>() {
            });
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> Set<T> toSet(String str, Class<T> clz) {
        try {
            return jsonMapper.readValue(str, TypeFactory.defaultInstance().constructCollectionType(Set.class, clz));
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static JsonNode toJsonNode(String str) {
        try {
            return jsonMapper.readTree(str);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static <T> T nodeToValue(JsonNode node, Class<T> clz) {
        try {
            return jsonMapper.treeToValue(node, clz);
        } catch (JsonProcessingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String toStr(Object t) {
        try {
            return jsonMapper.writeValueAsString(t);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String getValue(String json, String key) {

        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString(key);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] objectToBytes(Object obj) {
        try {
            return jsonMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> T bytesToObject(byte[] bytes, Class<T> clz) {
        try {
            return jsonMapper.readValue(bytes, clz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
