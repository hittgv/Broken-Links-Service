package io.restall.hittgv.broken;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Body {

    private final String url;

    private final String html;

    @JsonCreator
    public Body(@JsonProperty("url") String url, @JsonProperty("html") String html) {
        this.url = url;
        this.html = html;
    }

    public String getUrl() {
        return url;
    }

    public String getHtml() {
        return html;
    }
}
