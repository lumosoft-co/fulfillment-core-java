package com.agoramp;

import com.agoramp.controller.PollingController;
import com.agoramp.controller.WebhookController;
import com.agoramp.data.FulfillmentDestinationConfig;
import com.agoramp.error.ServiceAlreadyInitializedException;
import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public enum AgoraFulfillmentService {
    INSTANCE;

    @Getter(AccessLevel.PACKAGE)
    private FulfillmentDestinationConfig config;
    @Getter(AccessLevel.PACKAGE)
    private FulfillmentExecutor executor;

    public void initializeFromFile(File file, FulfillmentExecutor executor) throws FileNotFoundException, ServiceAlreadyInitializedException {
        initialize(new Gson().fromJson(new FileReader(file), FulfillmentDestinationConfig.class), executor);
    }

    public void initialize(FulfillmentDestinationConfig config, FulfillmentExecutor executor) throws ServiceAlreadyInitializedException {
        if (this.config != null) throw new ServiceAlreadyInitializedException();
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