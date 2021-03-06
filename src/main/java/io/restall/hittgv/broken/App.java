package io.restall.hittgv.broken;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.jackson.datatype.JavaslangModule;
import org.apache.commons.validator.routines.UrlValidator;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import spark.Request;

import static java.lang.System.*;
import static spark.Spark.*;

public class App {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaslangModule());
        AsyncHttpClient httpClient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent("Broken-Link-Checker conor@restall.io")
                        .setConnectTimeout(5000)
                        .setReadTimeout(1000)
                        .setIoThreadsCount(200).build()
        );

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);

        port(Integer.parseInt(getenv().get("PORT")));

        post("/", (Request req, spark.Response res) -> {
            Body body = objectMapper.readValue(req.bodyAsBytes(), Body.class);

            Document doc = Jsoup.parse(body.getHtml(), body.getUrl());

            Elements elements = doc.select("a[href]");

            Map<State, Integer> links = elements.stream().collect(Stream.collector())
                    .map(element -> element.attr("abs:href"))
                    .filter(urlValidator::isValid)
                    .map(url -> httpClient.prepareGet(url)
                            .execute()
                            .toCompletableFuture()
                            .thenApply(Response::getStatusCode)
                            .thenApply(State::fromStatusCode)
                            .thenApply(List::of)
                            .exceptionally(e -> List.of(State.FAIL)))
                    .reduce((a, b) -> a.thenCombineAsync(b, List::appendAll))
                    .thenApply(it -> it.groupBy(z -> z))
                    .get()
                    .map((a,b) -> Tuple.of(a, b.size()));

            Map<State, Integer> links2 = HashMap.empty();
            links2 = links2.put(State.SUCCESS, links.get(State.SUCCESS).getOrElse(0));
            links2 = links2.put(State.REDIRECT, links.get(State.REDIRECT).getOrElse(0));
            links2 = links2.put(State.FAIL, links.get(State.FAIL).getOrElse(0));

            return objectMapper.writeValueAsString(links2);
        });

    }

}
