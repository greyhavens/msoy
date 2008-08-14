//
// $Id$

package client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.HTML;

/**
 * Displays HTML entered by the user (be sure to validate all user-supplied HTML on the server
 * using HTMLSanitizer!). Rewrites any hrefs in that HTML to properly point to the top-level frame
 * so that they work in our multiframe client.
 */
public class SafeHTML extends HTML
{
    /**
     * Rewrites any anchors in the supplied DOM tree to properly point to the top-level frame where
     * appropriate.
     */
    public static void fixAnchors (Element element)
    {
        NodeList<Element> anchors = element.getElementsByTagName("a");
        for (int ii = 0; ii < anchors.getLength(); ii++) {
            Element anchor = anchors.getItem(ii);
            anchor.setAttribute("href", fixLink(anchor.getAttribute("href")));
            if (isWhirledLink(anchor.getAttribute("href"))) {
                anchor.setAttribute("target", "_top");
            } else {
                anchor.setAttribute("target", "_blank");
                if (anchor.getInnerHTML().indexOf("<img") == -1) {
                    anchor.setClassName("external");
                }
            }
        }
    }

    public SafeHTML (String html)
    {
        setHTML(html);
    }

    @Override // from HTML
    public void setHTML (String html)
    {
        super.setHTML(html);
        fixAnchors(getElement());
    }

    protected static boolean isWhirledLink (String href)
    {
        return href.startsWith("#") || href.startsWith("/") ||
            href.startsWith("http://www.whirled.com"); // TODO
    }

    protected static String fixLink (String href)
    {
        return href.startsWith("#") ? ("/" + href) : href;
    }
}
