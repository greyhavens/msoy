//
// $Id$

package client.facebook;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MarkupBuilder;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.shell.CShell;
import client.shell.Page;

/**
 * Displays Facebook stuff like a list of friends who have played whirled and some status.
 */
public class FacebookPage extends Page
{
    /**
     * Reparses the whole DOM tree looking for fbml elements.
     * TODO: take an element id, parsing the whole tree is expensive
     */
    public static void reparseXFBML ()
    {
        CShell.log("Reparsing XFBML");
        nreparseXFBML();
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("")) {
            // hello whirled
            FlowPanel panel = new FlowPanel();
            panel.add(MsoyUI.createLabel("Hello Whirled", "helloWhirled"));

            // Shanti's user name
            MarkupBuilder bldr = new MarkupBuilder();
            bldr.open("fb:serverfbml").open("script", "type", "text/fbml").open("fb:fbml");
            //bldr.open("fb:add-section-button", "section", "profile").append();
            bldr.open("fb:name", "uid", String.valueOf(507435487)).append();
            panel.add(MsoyUI.createHTML(bldr.finish(), null));
    
            setContent("Hello Whirled", panel);
            reparseXFBML();
        }
    }

    @Override // from Page
    public Pages getPageId ()
    {
        return Pages.FACEBOOK;
    }

    @Override // from Page
    protected boolean isTitlePage ()
    {
        return false;
    }

    protected static native void nreparseXFBML () /*-{
        try {
            $wnd.FB_ParseXFBML();
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to reparse XFBML [error=" + e + "]");
            }
        }
    }-*/;
}
