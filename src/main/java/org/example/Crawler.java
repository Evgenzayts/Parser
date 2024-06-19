package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Crawler {
    BlockingQueue<Article> resultArticles;
    String url;
    Document doc;

    Crawler(String url) {
        this.url = url;
        this.resultArticles = new LinkedBlockingQueue<>();
        try {
            this.doc = Jsoup.connect(this.url).userAgent("Mozilla").cookie("beget", "begetok").get();
        } catch (IOException e) {
            throw new RuntimeException("Could not connect to url: " + this.url);
        }
    }

    public BlockingQueue<Article> getArticles() {
        return resultArticles;
    }

    public void execute() throws InterruptedException {
        collectAllInfo();
    }

    private void collectAllInfo() {
        ExecutorService threadExecutor = Executors.newFixedThreadPool(3);

        Elements articleElements = doc.getElementsByClass("card-mini__title");
        Elements bigArticleElements = doc.getElementsByClass("card-big__title");
        articleElements.addAll(bigArticleElements);
        if (articleElements.isEmpty())
            throw new RuntimeException("No results for link: " + url);

        BlockingQueue<Element> linkQueue = new LinkedBlockingQueue<>(articleElements);

        while (!linkQueue.isEmpty()) {
            Element articleElement = linkQueue.poll();
            threadExecutor.execute(() -> {
                String link = getArticleLink(articleElement);
                Article article = collectArticleInfo(link);
                if (article != null) {
                    this.resultArticles.add(article);
                }
            });
        }

        threadExecutor.shutdown();
        try {
            threadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting threads: " + e);
        }
    }

    public void writeArticle() {
        resultArticles.forEach(article -> System.out.println(article.toString()));
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
