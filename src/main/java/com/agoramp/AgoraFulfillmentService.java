package com.agoramp;

import com.agoramp.controller.PollingController;
import com.agoramp.controller.WebhookController;
import com.agoramp.data.FulfillmentDestinationConfig;
import com.agoramp.error.ServiceAlreadyInitializedException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.*;

public enum AgoraFulfillmentService {
    INSTANCE;

    @Getter(AccessLevel.PACKAGE)
    private FulfillmentDestinationConfig config;
    @Getter(AccessLevel.PACKAGE)
    private FulfillmentExecutor executor;

    public void initializeFromFile(File file, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException, IOException {
        try {
            initialize(new Gson().fromJson(new FileReader(file), FulfillmentDestinationConfig.class), executor);
        } catch (FileNotFoundException e) {
            file.getParentFile().mkdirs();
            FulfillmentDestinationConfig config = new FulfillmentDestinationConfig(
                    "Get your secret from https://admin.agoramp.com/destinations",
                    -1
            );
            JsonObject json = new Gson()
                    .toJsonTree(config)
                    .getAsJsonObject();
            json.addProperty("port-comment", "Specifying the port field will register this destination for webhook support");
            FileWriter writer = new FileWriter(file);
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            writer.flush();
            writer.close();
            initialize(config, executor);
        }
    }

    public void initialize(FulfillmentDestinationConfig config, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException {
        if (this.config != null) throw new ServiceAlreadyInitializedException();
        if (config == null || config.getSecret() == null) throw new Error("Secret not defined");
        System.out.println("Agora fulfillment service initializing...");
        this.config = config;
        this.executor = executor;
        if (config.getPort() != null && config.getPort() >= 0) {
            WebhookController.INSTANCE.initialize(config.getPort(), config.getSecret());
        }
        PollingController.INSTANCE.initialize();
    }

    public void shutdown() {
        if (this.config == null) return;
        WebhookController.INSTANCE.shutdown();
    }
}
