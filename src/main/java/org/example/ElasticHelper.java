package org.example;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class ElasticHelper {
    private final ConnectionFactory factory;
    private final String queueResult;
    private Channel channel;
    private final ManElastic manager;

    public ElasticHelper(ConnectionFactory factory, String queryInfo, ManElastic manager) {
        this.factory = factory;
        this.queueResult = queryInfo;
        this.manager = manager;
    }

    private void process(GetResponse response) throws IOException {
        try {
            String messageBody = new String(response.getBody(), StandardCharsets.UTF_8);
            JSONObject jsonObjectInfo = new JSONObject(messageBody);
            Article article = Article.fromJsonString(jsonObjectInfo);

            boolean documentExists = manager.checkExistingArticle(article);

            if (!documentExists) {
                manager.addArticle(article);
            } else {
                System.err.println("Dublicate article");
            }
            channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
            System.err.println("Error response: " + e);
            channel.basicReject(response.getEnvelope().getDeliveryTag(), true);
        }
    }

    public void run() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();

        try {
            channel.queueDeclare(queueResult, false, false, false, null);

            GetResponse response = channel.basicGet(queueResult, false);
            while (response != null) {
                process(response);
                response = channel.basicGet(queueResult, false);
            }

        } catch (IOException e) {
            System.err.println("Error in run method", e);
        } finally {
            channel.close();
            connection.close();
        }
    }
}
