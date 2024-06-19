package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Queue {
    private static final String REQUESTS_QUEUE_NAME = "href_queue";
    private static final String RESULTS_QUEUE_NAME = "results_queue";
    Channel channel;

    Queue() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);

        // создание соединения
        Connection connection;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Error with RabbitMq connection creation: " + e);
        }

        try {
            channel.queueDeclare(REQUESTS_QUEUE_NAME, false, false, false, null);
            channel.queueDeclare(RESULTS_QUEUE_NAME, false, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException("Error with queue creation:" + e);
        }
    }

    public void fillRequestQueue() {

    }

//    public void Listen(DeliverCallback deliverCallback) {
//        try {
//            if (channel.isOpen()) {
//                channel.basicConsume(QUEUE_NAME,false, deliverCallback, consumerTag-> { });
//            } else {
//                System.out.println("closed channel input");
//            }
//        } catch (Exception e) {
//            System.out.println("Listen(DeliverCallback deliverCallback)");
//            System.out.println(e);
//            return;
//        }
//    }

    public Channel getChannel() {
        return channel;
    }
}
