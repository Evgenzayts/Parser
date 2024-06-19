package org.example;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import java.io.IOException;

public class ManElastic {
    private final ElasticsearchClient client;
    private static final String INDEX = "article";
    private static final String DB_LINK = "http://localhost:9200";

    public ManElastic() {
        this.client = new ElasticsearchClient(
                new RestClientTransport(
                        RestClient.builder(HttpHost.create(DB_LINK)).build(),
                        new JacksonJsonpMapper(JsonMapper.builder().build())
                )
        );
    }

    public void init() throws IOException {
        if (!checkExistingIndex()) {
            makeIndex();
        }
    }

    private boolean checkExistingIndex() throws IOException {
        BooleanResponse response = client.indices().exists(i -> i.index(INDEX));
        return response.value();
    }

    private void makeIndex() {
        try {
            client.indices().create(i -> i.index(INDEX)
                    .mappings(m -> m.properties("hash", p -> p.keyword(d -> d))
                            .properties("url", p -> p.text(d -> d))
                            .properties("title", p -> p.text(d -> d))
                            .properties("text", p -> p.text(d -> d))
                            .properties("author", p -> p.keyword(d -> d))
                            .properties("date", p -> p.date(d -> d))
                    ));
        } catch (IOException e) {
            System.err.println("Error creating index " + INDEX + ": " + e.getMessage());
        }
    }

    public boolean checkExistingArticle(Article article) throws IOException {
        SearchResponse<Object> searchResponse = client.search(builder ->
                        builder.index(INDEX)
                                .query(query -> query.term(termQuery -> termQuery.field("hash").value(article.getHash()))),
                Object.class);

        return searchResponse.hits().total().value() != 0;
    }

    public void addArticle(Article article) {
        try {
            IndexResponse response = client.index(index -> index
                    .index(INDEX)
                    .document(article));
        } catch (IOException e) {
            System.err.println("Error indexing document with hash " + article.getHash());
        }
    }

    public void close() {
        try {
            if (client != null) {
                ((RestClientTransport) client._transport()).restClient().close();
            }
        } catch (IOException e) {
            System.err.println("Error closing Elasticsearch client: " + e);
        }
    }
}