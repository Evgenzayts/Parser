package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        String url = "https://lenta.ru";

        Crawler crawler = new Crawler(url);
        try {
            crawler.execute();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while process: " + e);
        }
        crawler.writeArticle();
    }
}