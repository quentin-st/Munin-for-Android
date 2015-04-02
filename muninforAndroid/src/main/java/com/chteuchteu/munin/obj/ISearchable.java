package com.chteuchteu.munin.obj;

public interface ISearchable {
    public boolean matches(String expr);
    public String[] getSearchResult();
    public SearchResult.SearchResultType getSearchResultType();
}
