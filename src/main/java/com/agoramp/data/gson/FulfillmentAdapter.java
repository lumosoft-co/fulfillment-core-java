package com.agoramp.data.gson;

import com.agoramp.data.models.fulfillments.Fulfillment;
import com.agoramp.data.models.fulfillments.FulfillmentType;
import com.agoramp.util.DataUtil;
import com.google.gson.*;

import java.lang.reflect.Type;

public class FulfillmentAdapter implements JsonDeserializer<Fulfillment<?>> {
    @Override
    public Fulfillment<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        FulfillmentType type = FulfillmentType.valueOf(object.get("type").getAsString().toUpperCase());
        Object data = DataUtil.fromJson(object.get("data").toString(), type.getType());
        return new Fulfillment<>(
                object.get("id").getAsString(),
                type,
                data
        );
    }
}
