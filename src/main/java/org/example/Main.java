package org.example;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        String url = "https://lenta.ru";

        CommonCrawler commonCrawler = null;
        try {
            commonCrawler = new CommonCrawler(url);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Error init commonCrawler: " + e);
        }

        try {
            commonCrawler.execute();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error execution commonCrawler: " + e);
        }
    }
}