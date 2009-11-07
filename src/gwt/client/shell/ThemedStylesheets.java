//
// $Id: ScriptSources.java 18228 2009-10-01 18:17:40Z jamie $

package client.shell;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.NodeList;

/**
 * Allows index.html files for the top-level frame and pages to refer to stylesheet URL's on
 * the form /themed/css/foo.css?themeId=0 while we inject the user's theme as it loads/changes.
 */
public class ThemedStylesheets
{
    /**
     * Injects the src tags of all known scripts using the given application id where appropriate.
     */
    public static void inject (int themeId)
    {
        CShell.log("Looking for head...");
        NodeList<Element> heads = Document.get().getElementsByTagName("head");
        if (heads.getLength() == 0) {
            return;
        }
        CShell.log("Looking for links...");
        NodeList<Element> links = heads.getItem(0).getElementsByTagName("link");
        for (int ii = links.getLength()-1; ii >= 0; ii --) {
            LinkElement link = LinkElement.as(links.getItem(ii));
            CShell.log("Checking it's a stylesheet link...");
            if ("stylesheet".equalsIgnoreCase(link.getRel())) {
                String href = link.getHref();
                int ix = href.indexOf("?themeId=");
                if (ix > 0) {
                    href = (href.substring(0, ix) + "?themeId=" + themeId);
                    CShell.log("Changing href to: " + href);
                    link.setHref(href);
                }
            }
        }
    }
}
