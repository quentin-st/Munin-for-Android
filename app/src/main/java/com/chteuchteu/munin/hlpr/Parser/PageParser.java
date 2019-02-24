package com.chteuchteu.munin.hlpr.Parser;

import com.chteuchteu.munin.hlpr.Exception.Parser.EmptyResultSetException;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PageParser {
    public static final String GRAPH_IMAGE_SELECTOR = "img[src*=-day.]";

    /**
     * Parses specified HTML and tries to detect the page type.
     * Page types are defined in the PageType enum.
     */
    public static PageType detectPageType(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri);
        Elements images = doc.select(GRAPH_IMAGE_SELECTOR);

        // Found some, must be a plugins list page
        if (images.size() > 0) {
            return PageType.PLUGINS_LIST;
        }

        // Standard munin
        Elements muninHosts = doc.select("span.host");
        // MunStrap
        Elements munstrapHosts = doc.select("ul.groupview");

        if (muninHosts.size() > 0 || munstrapHosts.size() > 0) {
            return PageType.NODES_LIST;
        }

        // Could not deduce anything
        return null;
    }

    /**
     * Deduces a pretty master name from the specified HTML.
     * We expect the html argument to be the nodes list page.
     *
     * Returns null if we cannot deduce one. In that case, use MuninMaster.deduceName().
     */
    public static String findMasterName(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri);

        String domainsSelector = html.contains("MunStrap")
            ? "ul.groupview > li > a.link-domain" // MunStrap
            : "span.domain"; // Standard munin

        Elements domains = doc.select(domainsSelector);

        if (domains.size() == 0) {
            // Could not find any domain in that page!
            return null;
        }

        if (domains.size() > 1) {
            // Several domains, can't deduce from that!
            return null;
        }

        String domainName = domains.get(0).text();

        if (domainName.equals("localdomain")) {
            // That's the default localdomain, let's not use that
            return null;
        }

        return domains.get(0).text();
    }

    /**
     * Finds nodes in the specified HTML result.
     * We expect the baseUri to be master's URL.
     * It's your duty to attach nodes, and to update their position.
     */
    public static ArrayList<MuninNode> parseNodes(MuninMaster master, String html) throws EmptyResultSetException {
        Document doc = Jsoup.parse(html, master.getUrl());

        ArrayList<MuninNode> nodes = new ArrayList<>();

        // Check if Munin or MunStrap
        if (html.contains("MunStrap")) { // MunStrap
            Elements domains = doc.select("ul.groupview > li > a.link-domain");

            if (domains.size() == 0) {
                throw new EmptyResultSetException();
            }

            for (Element domain : domains) {
                // Get every host for that domain
                Elements hosts = domain.parent().select("ul>li");
                for (Element host : hosts) {
                    Elements hostLinks = host.select("a.link-host");

                    if (hostLinks.size() == 0)
                        continue;

                    Element hostLink = hostLinks.get(0);
                    String nodeName = hostLink.text();
                    String nodeUrl = hostLink.attr("abs:href");
                    nodes.add(new MuninNode(nodeName, nodeUrl));
                }
            }
        } else { // Munin
            Elements domains = doc.select("span.domain");

            if (domains.size() == 0) {
                throw new EmptyResultSetException();
            }

            for (Element domain : domains) {
                // Get every host for that domain
                Elements hosts = domain.parent().select("span.host");

                for (Element host : hosts) {
                    String nodeName = host.child(0).text();
                    String nodeUrl = host.child(0).attr("abs:href");

                    nodes.add(new MuninNode(nodeName, nodeUrl));
                }
            }
        }

        return nodes;
    }

    public static ArrayList<MuninPlugin> parsePlugins(MuninNode muninNode, String html) {
        Document doc = Jsoup.parse(html, muninNode.getUrl());

        ArrayList<MuninPlugin> plugins = new ArrayList<>();

        Pattern pluginNamePattern = Pattern.compile("/([^/]*)-day\\..*");

        for (Element image : doc.select(GRAPH_IMAGE_SELECTOR)) {
            String imageSrc = image.attr("src");

            Matcher pluginNameMatcher = pluginNamePattern.matcher(imageSrc);
            if (!pluginNameMatcher.find())
                throw new RuntimeException("Could not extract plugin name from URL " + imageSrc);
            String pluginName = pluginNameMatcher.group(1);

            // Delete special chars
            pluginName = Util.removeAll(pluginName, new String[]{
                "&", "^", "\"", ",", ";"
            });

            String fancyName = image.attr("alt");
            // Delete quotes
            fancyName = fancyName.replaceAll("\"", "");

            // Get graphUrl
            Element link = image.parent();
            String pluginPageUrl = link.attr("abs:href");

            // Get groupName
            String group = "";
            if (html.contains("MunStrap")) {
                Element tab = image.parent().parent().parent().parent();
                group = tab.id();
            } else {
                // Munin 2.X
                boolean is2 = true;

                if (html.contains("<table")) {
                    Element table = image.parent().parent().parent().parent().parent();

                    if (table != null) {
                        Element h3 = table.previousElementSibling();
                        if (h3 != null)
                            group = h3.html();
                        else
                            is2 = false;
                    } else
                        is2 = false;
                } else {
                    // munin 2.999/3.0 redesign: removed tables
                    Element container = image.parent().parent().parent().parent();
                    if (container != null && container.hasAttr("data-category"))
                        group = container.attr("data-category");
                    else is2 = false;
                }

                // Munin 1.4
                if (!is2) {
                    try {
                        Element h3 = image.parent().parent().parent().parent().child(0).child(0).child(0);
                        group = h3.html();
                    }
                    catch (Exception e) { e.printStackTrace(); }
                }
            }

            MuninPlugin plugin = new MuninPlugin(pluginName, muninNode);
            plugin.setFancyName(fancyName);
            plugin.setCategory(group);
            plugin.setPluginPageUrl(pluginPageUrl);
            plugin.setPosition(plugins.size());

            plugins.add(plugin);

            // Find GraphURL
            if (muninNode.getGraphURL().equals("")) {
                String srcAttr = image.attr("abs:src");
                muninNode.setGraphURL(srcAttr.substring(0, srcAttr.lastIndexOf('/') + 1));
            }
        }

        return plugins;
    }
}
