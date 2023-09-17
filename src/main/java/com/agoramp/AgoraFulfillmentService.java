package com.agoramp;

import com.agoramp.controller.PollingController;
import com.agoramp.controller.Storefront;
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

    public boolean initializeFromFile(File file, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException, IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            FulfillmentDestinationConfig config = new FulfillmentDestinationConfig(
                    "Get your secret from https://admin.agoramp.com/destinations",
                    0
            );
            JsonObject json = new Gson()
                    .toJsonTree(config)
                    .getAsJsonObject();
            json.addProperty("port-comment", "Set the port field to -1 to disable webhooks");
            FileWriter writer = new FileWriter(file);
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            writer.flush();
            writer.close();
            return false;
        }
        return initialize(new Gson().fromJson(new FileReader(file), FulfillmentDestinationConfig.class), executor);
    }

    public boolean initialize(FulfillmentDestinationConfig config, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException {
        if (this.config != null) throw new ServiceAlreadyInitializedException();
        if (config == null || config.getSecret() == null) return false;
        System.out.println("Agora fulfillment service initializing...");
        this.config = config;
        this.executor = executor;
        Storefront.INSTANCE.initialize(config.getSecret());
        if (config.getPort() != null && config.getPort() >= 0) {
            WebhookController.INSTANCE.initialize(config.getPort(), config.getSecret());
        }
        PollingController.INSTANCE.initialize();
        return true;
    }

    public void shutdown() {
        if (this.config == null) return;
        WebhookController.INSTANCE.shutdown();
        PollingController.INSTANCE.shutdown();
    }
}
