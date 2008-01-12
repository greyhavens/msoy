//
// $Id$

package client.wrap;

import com.google.gwt.user.client.ui.Frame;

import client.shell.Args;
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
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("w") && args.getArgCount() == 0) {
            displayPage("Wiki", "http://wiki.whirled.com/");
        } else if (action.equals("w")) {
            displayPage("Wiki", "http://wiki.whirled.com/" + Args.compose(args.splice(1)));
        } else if (action.equals("about")) {
            displayPage("About Whirled", "about.html");
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "wrap";
    }

    protected void displayPage (String title, String url)
    {
        client.shell.Frame.setTitle(title);
        Frame frame = new Frame(url);
        frame.setStyleName("wrappedFrame");
        setContent(frame);
    }
}
