package io.restall.hittgv.broken;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.Tuple;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.jackson.datatype.JavaslangModule;
import org.apache.commons.validator.routines.UrlValidator;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import static spark.Spark.*;

public class App {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaslangModule());
        AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);

        post("/", (req, res) -> {
            Body body = objectMapper.readValue(req.bodyAsBytes(), Body.class);

            Document doc = Jsoup.parse(body.getHtml(), body.getUrl());

            Elements elements = doc.select("a[href]");

            Map<State, Integer> links = elements.stream().collect(Stream.collector())
                    .map(element -> element.attr("abs:href"))
                    .filter(urlValidator::isValid)
                    .map(url -> httpClient.prepareGet(url)
                            .execute()
                            .toCompletableFuture()
                            .exceptionally(e -> null)
                            .thenApply(Response::getStatusCode)
                            .thenApply(State::fromStatusCode)
                            .thenApply(List::of))
                    .reduce((a, b) -> a.thenCombineAsync(b, List::appendAll))
                    .thenApply(it -> it.groupBy(z -> z))
                    .get()
                    .map((a,b) -> Tuple.of(a, b.size()));

            return objectMapper.writeValueAsString(links);
        });

    }

}
