package dev.aga;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import dev.aga.sqs.Consumer;
import dev.aga.sqs.Producer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        var client = buildSqsClient();
        var queueA = createQueue("testA.fifo", client);
        var queueB = createQueue("testB.fifo", client);
        var consumerA = new Consumer("A", client, queueA, queueB);
        var ackConsumer = new Consumer("B", client, queueB, null);
        var producer = new Producer(client, queueA);

        var pool = Executors.newFixedThreadPool(3);

        CompletableFuture.runAsync(producer, pool);
        CompletableFuture.runAsync(consumerA, pool);
        CompletableFuture.runAsync(ackConsumer, pool);
    }

    private static String createQueue(String name, AmazonSQS client) {
        var queueRequest = new CreateQueueRequest(name);
        queueRequest.setAttributes(getQueueAttributes());

        var result = client.createQueue(queueRequest);

        return result.getQueueUrl();
    }

    private static Map<String, String> getQueueAttributes() {
        return Map.of(
                "DelaySeconds", "0",
                "FifoQueue", "true",
                "ContentBasedDeduplication", "true",
                "VisibilityTimeout", "60",
                "MaximumMessageSize", "262144",
                "MessageRetentionPeriod", "604800",
                "ReceiveMessageWaitTimeSeconds", "10"
        );
    }

    private static AmazonSQS buildSqsClient() {
        return AmazonSQSClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:4566", "us-east-1"))
                .build();
    }
}
