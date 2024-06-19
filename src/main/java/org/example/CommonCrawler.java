package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommonCrawler {
    private final String url;
    ConnectionFactory factory;
    public static final String QUEUE_LINK = "QUERY_LINK";
    public static final String QUEUE_RESULT = "QUEUE_RESULT";

    CommonCrawler(String url) throws IOException, TimeoutException {
        this.url = url;

        this.factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_LINK, false, false, false, null);
        channel.queueDeclare(QUEUE_RESULT, false, false, false, null);
        channel.close();
        connection.close();
    }

    public void execute() throws InterruptedException {
        fillLinkQueue();

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            executorService.submit(new ArticleReader(factory, QUEUE_LINK, QUEUE_RESULT));
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        ManElastic manager = new ManElastic();
        try {
            manager.init();
            ElasticHelper information = new ElasticHelper(factory, QUEUE_RESULT, manager);
            information.run();
        } finally {
            manager.close();
        }

    }

    private void fillLinkQueue() {
        Document doc;
        try {
            doc = Jsoup.connect(this.url).get();
        } catch (IOException e) {
            throw new RuntimeException("Could not connect to url: " + this.url);
        }
        Elements articleElements = doc.getElementsByClass("card-mini__title");
        Elements bigArticleElements = doc.getElementsByClass("card-big__title");
        articleElements.addAll(bigArticleElements);
        if (articleElements.isEmpty())
            throw new RuntimeException("No results for link: " + url);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            for (Element articleElement : articleElements) {
                String link = getArticleLink(articleElement);

                channel.basicPublish("", QUEUE_LINK, null, link.getBytes());
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Could not to fill link queue: " + e);
        }
    }

    private String getArticleLink(Element articleElement) {
        if (articleElement.parent() == null || articleElement.parent().parent() == null) {
            return null;
        }

        String href;
        String sub_href = articleElement.parent().parent().attr("href");
        if (sub_href.isEmpty())
            return null;

        if (sub_href.startsWith("http")) {
            href = sub_href;
        } else {
            href = url + sub_href;
        }

        return href;
    }
}
