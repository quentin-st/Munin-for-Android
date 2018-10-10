package com.chteuchteu.munin.hlpr;

public final class HTMLParser {
    /**
     * Matches all -day graphs in the page. We exclude file extension on purpose
     * to match PNG & SVG extensions
     */
    public static final String MUNIN_GRAPH_SELECTOR = "img[src*=-day.]";
}
