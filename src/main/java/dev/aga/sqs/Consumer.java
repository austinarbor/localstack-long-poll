package dev.aga.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class Consumer implements Runnable {

    private final String name;
    private final AmazonSQS client;
    private final String queueA;
    private final Producer producer;

    public Consumer(String name, AmazonSQS client, String queueA, String ackQueue) {
        this.name = name;
        this.client = client;
        this.queueA = queueA;
        if (ackQueue != null) {
            this.producer = new Producer(client, ackQueue);
        } else {
            this.producer = null;
        }
    }

    @Override
    public void run() {
        System.out.printf("Starting Consumer %s\n", this.name);
        while (true) {
            long start = System.currentTimeMillis();
            var result = this.client.receiveMessage(getReceiveMessageRequest());
            long finish = System.currentTimeMillis();
            System.out.printf("(Consumer %s) Poll took %d ms\n", this.name, (finish - start));
            var messages = result.getMessages();
            for (var msg : messages) {
                System.out.printf("(Consumer %s) Received: %s\n", this.name, msg.getBody());
                if (this.producer != null) {
                    this.producer.sendMessage("ack: " + msg.getBody());
                }
                this.client.deleteMessage(this.queueA, msg.getReceiptHandle());
            }
        }
    }

    private ReceiveMessageRequest getReceiveMessageRequest() {
        var receiveRequest = new ReceiveMessageRequest(this.queueA);
        receiveRequest.setMaxNumberOfMessages(10);
        receiveRequest.setWaitTimeSeconds(20);
        receiveRequest.setVisibilityTimeout(60);
        return receiveRequest;
    }
}
