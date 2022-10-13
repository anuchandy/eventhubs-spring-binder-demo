package com.anut.eh;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class App {
    private static final String EH_CONNECTION_STRING = System.getenv("EH_CONNECTION_STRING");
    private static final String EH_CONSUMER_GROUP = "$Default";
    private static final String STG_CONNECTION_STRING = System.getenv("STORAGE_CONNECTION_STRING");
    private static final String STG_CONTAINER_NAME = System.getenv("STG_CONTAINER_NAME");
    public static final Consumer<EventContext> PARTITION_PROCESSOR = eventContext -> {
        System.out.printf("Processing event from partition %s with sequence number %d %n",
                eventContext.getPartitionContext().getPartitionId(), eventContext.getEventData().getSequenceNumber());
        if (eventContext.getEventData().getSequenceNumber() % 10 == 0) {
            eventContext.updateCheckpoint();
        }
    };

    public static final Consumer<ErrorContext> ERROR_HANDLER = errorContext -> {
        System.out.printf("Error occurred in partition processor for partition %s, %s.%n",
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable());
    };

    /**
     * The main method to run the sample.
     *
     * @param args Unused arguments to the sample.
     * @throws Exception if there are any errors while running the sample program.
     */
    public static void main(String[] args) throws Exception {
        BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder()
                .connectionString(STG_CONNECTION_STRING)
                .containerName(STG_CONTAINER_NAME)
                .buildAsyncClient();

        EventProcessorClientBuilder eventProcessorClientBuilder = new EventProcessorClientBuilder()
                .connectionString(EH_CONNECTION_STRING)
                .consumerGroup(EH_CONSUMER_GROUP)
                .processEvent(PARTITION_PROCESSOR)
                .processError(ERROR_HANDLER)
                .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient));

        EventProcessorClient eventProcessorClient = eventProcessorClientBuilder.buildEventProcessorClient();
        // Starts the event processor
        eventProcessorClient.start();

        // Perform other tasks while the event processor is processing events in the background.
        TimeUnit.MINUTES.sleep(5);

        // Stops the event processor
        eventProcessorClient.stop();
    }
}
