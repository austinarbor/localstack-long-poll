package dev.aga.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Producer implements Runnable {

    private static final String MESSAGE_GROUP_ID = "1";

    private final AmazonSQS client;
    private final String queueUrl;
    private final String queueName;


    public Producer(AmazonSQS client, String queueUrl) {
        this.client = client;
        this.queueUrl = queueUrl;
        int index = queueUrl.lastIndexOf('/');
        this.queueName = queueUrl.substring(index + 1);
    }

    public SendMessageResult sendMessage(String body) {
        var sendMessageRequest = new SendMessageRequest(this.queueUrl, body);
        sendMessageRequest.setMessageGroupId(MESSAGE_GROUP_ID);
        int newDepth = getQueueDepth();
        System.out.printf("(Producer) Putting message on queue '%s': %s. Current Depth: %d\n", this.queueName, body, newDepth);
        return this.client.sendMessage(sendMessageRequest);
    }

    private int getQueueDepth() {
        var req = new GetQueueAttributesRequest(this.queueUrl, List.of("ApproximateNumberOfMessages"));
        var resp = this.client.getQueueAttributes(req);
        var num = resp.getAttributes().get("ApproximateNumberOfMessages");
        if (num != null) {
            return Integer.parseInt(num);
        }
        return 0;
    }

    @Override
    public void run() {
        while (true) {
            var body = ThreadLocalRandom.current().nextInt();
            var result = sendMessage(Integer.toString(body));

            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 3000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
