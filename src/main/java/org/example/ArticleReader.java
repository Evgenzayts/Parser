package org.example;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ArticleReader implements Runnable {

    private final ConnectionFactory factory;
    private final String linkQueue;
    private final String resultQueue;
    private Channel channel;

    public ArticleReader(ConnectionFactory factory, String linkQueue, String resultQueue) {
        this.factory = factory;
        this.linkQueue = linkQueue;
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        try {
            Connection connection = factory.newConnection();
            this.channel = connection.createChannel();

            try {
                channel.queueDeclare(linkQueue, false, false, false, null);

                GetResponse response = channel.basicGet(linkQueue, false);
                while (response != null) {
                    String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String link = jsonResponse.getString("link");
                    Article article = collectArticleInfo(link);

                    channel.basicPublish("", resultQueue, null, article.toString().getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    response = channel.basicGet(linkQueue, false);

                    System.out.println(article.toString());
                }

            } catch (IOException e) {
                System.err.println("Error when reading link: " + e);
            } finally {
                channel.close();
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Error in RabbitMQ connection/channel: " + e);
        }
    }

    private Article collectArticleInfo(String link) {
        Article article = new Article();
        article.setLink(link);

        Document articleDetails;
        try {
            articleDetails = Jsoup.connect(link).get();
        } catch (IOException e) {
            System.err.println("Could not connect to url: " + link);
            return null;
        }

        String title = collectArticleTitleInfo(articleDetails, link);
        if (title != null)
            article.setTitle(title);

        List<String> authors = collectArticleAuthorInfo(articleDetails, link);
        if (authors != null)
            article.setAuthors(authors);

        String date = collectArticleDateInfo(articleDetails, link);
        if (date != null)
            article.setDate(date);

        article.createHash();

        return article;
    }

    private String collectArticleTitleInfo(Document articleDetails, String href) {
        String title = articleDetails.getElementsByClass("topic-body__title").text();
        if (title.isEmpty()) {
            title = articleDetails.getElementsByClass("premium-header__title").text();
            if (title.isEmpty()) {
                title = articleDetails.getElementsByClass("jsx-2514688684 Cqvs5c42").text();
                if (title.isEmpty()) {
                    title = articleDetails.getElementsByClass("jsx-4056589090 Cqvs5c42").text();
                    if (title.isEmpty()) {
                        System.err.println("Could not find title for: " + href);
                        return null;
                    }
                }
            }
        }

        return title;
    }

    private List<String> collectArticleAuthorInfo(Document articleDetails, String href) {
        Elements authorElements = articleDetails.getElementsByClass("topic-authors__author");
        if (authorElements.isEmpty()) {
            authorElements = articleDetails.getElementsByAttributeValue("data-qa", "authors-link");
            if (authorElements.isEmpty()) {
                System.err.println("Could not find authors for: " + href);
                return null;
            }
        }

        List<String> authors = new ArrayList<>();
        authorElements.forEach(authorElement -> authors.add(authorElement.text()));
        return authors;
    }

    private String collectArticleDateInfo(Document articleDetails, String href) {
        String date = articleDetails.getElementsByClass("topic-header__item topic-header__time").text();
        if (date.isEmpty()) {
            date = articleDetails.getElementsByClass("premium-header__time").text();
            if (date.isEmpty()) {
                date = articleDetails.getElementsByClass("qzByRHub P5lPq1qA").text();
                date = date.substring(date.indexOf(' ') + 1);
                if (date.isEmpty()) {
                    System.err.println("Could not find date for: " + href);
                    return null;
                }
            }
        }

        return date;
    }
}
