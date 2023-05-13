package com.agoramp.controller;

import com.agoramp.error.GraphQLError;
import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.rx3.Rx3Apollo;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public enum Storefront {
    INSTANCE;

    private ApolloClient client;

    public void initialize(String secret) {
        String shopId = getShopId(secret);
        if (shopId == null) {
            throw new Error("Could not find shop linked to this deployment");
        }
        client = new ApolloClient.Builder()
                .serverUrl("https://api.agoramp.com/graphql")
                .addHttpHeader("X-Agora-Store-ID", shopId)
                .build();
    }

    public <T extends Query.Data> Mono<T> query(Query<T> query) {
        return doCall(client.query(query));
    }

    public <T extends Mutation.Data> Mono<T> mutate(Mutation<T> mutation) {
        return doCall(client.mutation(mutation));
    }

    private <T extends Operation.Data> Mono<T> doCall(ApolloCall<T> call) {
        return Mono
                .fromCompletionStage(Rx3Apollo.single(call).toCompletionStage())
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(r -> {
                    if (r.hasErrors()) {
                        throw new GraphQLError(r.errors);
                    } else {
                        return r.data;
                    }
                });
    }

    private String getShopId(String secret) {
        try {
            URL url = new URL("https://api.agoramp.com/shop/" + secret);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String body = response.toString();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return body;
            } else {
                System.out.println("Error: Response code is not 200. \n\tReceived: " + responseCode + "\n\tBody: " + body);
            }
        } catch (Throwable t) {
            System.out.println("Could not retrieve shop id from API.");
            t.printStackTrace();
        }
        return null;
    }
}
