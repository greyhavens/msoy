//
// $Id$

package client.wrap;

import com.google.gwt.user.client.ui.Frame;

import client.shell.Page;

/**
 * Wraps the wiki and forums in an iframe.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        if (token.equals("w")) {
            displayPage("Wiki", "http://wiki.whirled.com/");

        } else if (token.startsWith("w-")) {
            displayPage("Wiki", "http://wiki.whirled.com/" + token.substring(2));

        } else if (token.equals("f")) {
            displayPage("Forums", "http://forums.whirled.com/");
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "wrap";
    }

    protected void displayPage (String title, String url)
    {
        setPageTitle(title);
        Frame frame = new Frame(url);
        frame.setWidth("99%");
        frame.setHeight("100%");
        setContent(frame);
        setContentStretchHeight(true);
    }
}
