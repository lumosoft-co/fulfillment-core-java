package com.agoramp.controller;

import com.agoramp.error.GraphQLError;
import com.apollographql.apollo3.api.*;
import com.apollographql.apollo3.api.json.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import okio.Buffer;
import okio.BufferedSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

import java.io.*;
import java.lang.Error;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Storefront {
    INSTANCE;

    private String shopId;

    public void initialize(String secret) {
        shopId = getShopId(secret);
        if (shopId == null || shopId.isEmpty()) {
            throw new Error("Could not find shop linked to this deployment");
        }
        System.out.println(String.format("Loaded storefront connection to shop (%s)", shopId));
    }

    public <T extends Query.Data> Mono<T> query(Query<T> operation) {
        return call(operation, "query");
    }

    public <T extends Mutation.Data> Mono<T> mutate(Mutation<T> operation) {
        return call(operation, "mutation");
    }

    public <T extends Operation.Data> Mono<T> call(Operation<T> operation, String type) {
        Buffer buffer = new Buffer();
        try {
            Operations.composeJsonRequest(operation, new BufferedSinkJsonWriter(buffer, "  "));
        } catch (Exception e) {
            return Mono.error(e);
        }
        return HttpClient.create()
                .headers(h -> h
                        .add("X-Agora-Store-ID", shopId)
                        .add("Content-Type", "application/json"))
                .post()
                .uri("https://api.agoramp.com/graphql")
                .send(ByteBufMono.fromString(Mono.just(buffer.readUtf8())))
                .response((r, b) -> b.aggregate().asString(StandardCharsets.UTF_8))
                .singleOrEmpty()
                .map(json -> Operations.parseJsonResponse(operation, json))
                .map(n -> {
                    if (n.hasErrors()) {
                        throw new GraphQLError(operation, n.errors);
                    } else {
                        return n.dataAssertNoErrors();
                    }
                })
                .publishOn(Schedulers.single());
    }

    private Map<String, ?> convert(JsonObject json) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            out.put(entry.getKey(), convert(entry.getValue()));
        }
        return out;
    }

    private Object convert(JsonElement json) {
        if (json.isJsonObject()) {
            return convert(json.getAsJsonObject());
        } if (json.isJsonArray()) {
            List<Object> val = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                val.add(convert(element));
            }
            return val;
        } else if (json.isJsonPrimitive()) {
            JsonPrimitive p = json.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isString()) return p.getAsString();
            return new JsonNumber(p.getAsString());
        } else {
            return null;
        }
    }

    private String getShopId(String secret) {
        try {
            URL url = new URL("https://api.agoramp.com/shop/" + secret);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return read(connection.getInputStream());
            } else {
                String body = read(connection.getErrorStream());
                System.out.println("Error: Response code is not 200. \n\tReceived: " + responseCode + "\n\tBody: " + body);
            }
        } catch (Throwable t) {
            System.out.println("Could not retrieve shop id from API.");
            t.printStackTrace();
        }
        return null;
    }

    private String read(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();

    }
}
