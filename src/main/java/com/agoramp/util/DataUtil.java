package com.agoramp.util;

import com.agoramp.data.gson.FulfillmentAdapter;
import com.agoramp.data.models.fulfillments.Fulfillment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;

public class DataUtil {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Fulfillment.class, new FulfillmentAdapter())
            .create();

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    public static byte[] toJson(Object value) {
        return GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
    }
}
