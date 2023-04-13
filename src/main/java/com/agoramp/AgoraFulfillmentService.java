package com.agoramp;

import com.agoramp.controller.PollingController;
import com.agoramp.controller.WebhookController;
import com.agoramp.data.FulfillmentDestinationConfig;
import com.agoramp.error.ServiceAlreadyInitializedException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
            FulfillmentDestinationConfig config = new FulfillmentDestinationConfig(null);
            String json = new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(config);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.flush();
            writer.close();
            initialize(config, executor);
        }
    }

    public void initialize(FulfillmentDestinationConfig config, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException {
        if (this.config != null) throw new ServiceAlreadyInitializedException();
        if (config == null || config.getSecret() == null) throw new Error("Secret not defined");
        this.config = config;
        this.executor = executor;
        if (config.getPort() != null) {
            WebhookController.INSTANCE.initialize(config.getPort());
        }
        PollingController.INSTANCE.initialize();
    }

    public void shutdown() {
        if (this.config == null) return;
        WebhookController.INSTANCE.shutdown();
    }
}