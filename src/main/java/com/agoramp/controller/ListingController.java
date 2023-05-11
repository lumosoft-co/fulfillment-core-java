package com.agoramp.controller;

import com.agoramp.data.models.graphql.ShopQuery;
import com.agoramp.error.GraphQLError;
import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.rx3.Rx3Apollo;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.io.IOException;

public enum ListingController {
    INSTANCE;

    private ApolloClient client;
    private ShopQuery.Shop shop;

    public void initialize(String secret) throws IOException {
        String shopId = getShopId(secret);
        client = new ApolloClient.Builder()
                .serverUrl("https://api.agoramp.com/graphql")
                .addHttpHeader("X-Agora-Store-ID", shopId)
                .build();
        ShopQuery.Data data = query(new ShopQuery()).block();
        if (data == null) {
            throw new Error("Could not find shop linked to this deployment");
        } else {
            shop = data.shop;
        }
    }

    public <T extends Query.Data> Mono<T> query(Query<T> query) {
        ApolloCall<T> call = client.query(query);
        Single<ApolloResponse<T>> response = Rx3Apollo.single(call);
        return Mono
                .fromCompletionStage(response.toCompletionStage())
                .mapNotNull(r -> {
                    if (r.hasErrors()) {
                        throw new GraphQLError(r.errors);
                    } else {
                        return r.data;
                    }
                });
    }

    private String getShopId(String secret) throws IOException {
        URL url = new URL("https://api.agoramp.com/shop/" + secret);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } else {
            throw new IOException("Error: Response code is not 200. Received: " + responseCode);
        }
    }
}
