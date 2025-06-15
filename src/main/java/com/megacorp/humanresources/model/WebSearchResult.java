package com.megacorp.humanresources.model;

public class WebSearchResult {

    private String rank;
    private String title;
    private String url;
    private String snippet;

    public WebSearchResult(String rank, String title, String url, String snippet) {
        this.rank = rank;
        this.title = title;
        this.url = url;
        this.snippet = snippet;
    }

    public String getRank() {
        return rank;
    }
    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
    @Override
    public String toString() {
        return "WebSearchResult{" +
                "rank=" + rank +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", snippet='" + snippet + '\'' +
                '}';
    }

}