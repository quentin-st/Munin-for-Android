package com.chteuchteu.munin.hlpr.Parser;

import android.net.Uri;

import com.chteuchteu.munin.hlpr.Dynazoom.DynazoomAvailability;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynazoomDiscoveryHelper {
    /**
     * Checks dynazoom availability starting from the specified node.
     * If anything goes wrong, we'll assume it's not.
     *
     * MuninNode.hdGraphUrl will be updated on success
     */
    public static void checkDynazoomAvailability(MuninNode node, String userAgent) {
        MuninMaster master = node.getParent();
        String nodeUrl = node.getUrl();

        try {
            HTMLResponse response = node.getParent().downloadUrl(nodeUrl, userAgent);

            // Throw if request went wrong
            response.throwOnFailure();

            // To go to the dynazoom page, we have to "click" on the first graph.
            // Then, on the second page, we have to "click" again on the first graph.
            // With multigraph feature, we have to click once more.
            // Finally, the only image on this third page is the dynazoom graph.

            Document doc = Jsoup.parse(response.getHtml(), nodeUrl);
            Elements images = doc.select(PageParser.GRAPH_IMAGE_SELECTOR);
            Element image = images.get(0);

            String subPageUrl = image.parent().attr("abs:href");
            boolean dynazoomPageReached = false;
            int subLevel = 0,
                maxLevels = 4;

            while (!dynazoomPageReached) {
                HTMLResponse subPageResponse = master.downloadUrl(subPageUrl, userAgent);

                // Throw if request went wrong
                subPageResponse.throwOnFailure();

                String subPageHtml = subPageResponse.getHtml();
                Document subPage = Jsoup.parse(subPageHtml, subPageUrl);
                dynazoomPageReached = subPageHtml.contains("Zooming is");

                if (dynazoomPageReached) {
                    // Since the image URL is built in JS on the web page, we have to build it manually
                    // Parse page URL
                    Uri uri = Uri.parse(subPageUrl);
                    String cgiUrl = uri.getQueryParameterNames().contains("cgiurl_graph")
                        ? uri.getQueryParameter("cgiurl_graph")
                        : "/munin-cgi/munin-cgi-graph";
                    if (!cgiUrl.endsWith("/"))
                        cgiUrl += "/";

                    // localdomain/localhost.localdomain/if_eth0
                    String pluginNameUrl = uri.getQueryParameterNames().contains("plugin_name")
                        ? uri.getQueryParameter("plugin_name")
                        : "localdomain/localhost.localdomain/pluginName";

                    // Remove plugin name from pluginNameUrl
                    // Get prefix from path:
                    // group/node/[multigraph_name/]/plugin_name
                    Pattern pattern = Pattern.compile("^(((?:[^/])*/){2}).*");
                    Matcher matcher = pattern.matcher(pluginNameUrl);

                    if (!matcher.find())
                        throw new RuntimeException("Could not determine usable pluginNameUrl from " + pluginNameUrl);

                    pluginNameUrl = matcher.group(1);

                    String hdGraphURL = Util.URLManipulation.getScheme(nodeUrl)
                        + Util.URLManipulation.getHostFromUrl(nodeUrl)
                        + ":" + Util.URLManipulation.getPort(nodeUrl)
                        + cgiUrl + pluginNameUrl;
                    node.setHdGraphURL(hdGraphURL);

                    // Now that we have the HD Graph URL, let's try to reach it to see if it is available
                    if (master.isDynazoomAvailable(userAgent))
                        master.setDynazoomAvailable(DynazoomAvailability.TRUE);
                    else
                        master.setDynazoomAvailable(DynazoomAvailability.FALSE);
                } else {
                    // We haven't reached dynazoom page yet, find the first graph to click on
                    Element graph = subPage.select(PageParser.GRAPH_IMAGE_SELECTOR).get(0);

                    subPageUrl = graph.parent().attr("abs:href");
                    // Loop over
                }

                // Avoid infinite loop
                subLevel++;
                if (subLevel > maxLevels) {
                    throw new RuntimeException("Max level (" + maxLevels + ") reached: " + subLevel);
                }
            }
        } catch (Exception ex) {
            // Parsing pages is quite tricky, especially when the server configuration may be wrong.
            master.setDynazoomAvailable(DynazoomAvailability.FALSE);
        }
    }
}
