package org.example;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Article {
    String title;
    String date;
    List<String> authors;
    String link;
    String hash;

    public List<String> getAuthors() {
        return authors;
    }

    public String getHash() {
        return hash;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getLink() {
        return link;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void createHash() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] messageDigest = md.digest((this.title + this.link).getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        this.hash = no.toString(16);
    }

    @Override
    public String toString() {
        return "Article (" + this.link + ") {" +
                "title='" + this.title + '\'' +
                ", date='" + this.date + '\'' +
                ", authors='" + this.authors + '\'' +
                ", hash='" + this.hash + '\'' +
                "}";
    }
}
