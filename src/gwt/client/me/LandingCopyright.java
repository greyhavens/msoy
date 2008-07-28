//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.user.client.ui.FlowPanel;

import client.shell.Page;
import client.ui.MsoyUI;
import client.util.DateUtil;
import client.util.Link;

/**
 * Displays a summary of what Whirled is, featuring games, avatars and whirleds.
 * Spans the entire width of the page, with an active content area 800 pixels wide and centered.
 */
public class LandingCopyright extends FlowPanel
{
    public LandingCopyright ()
    {
        // copyright, about, terms & conditions, help
        setStyleName("LandingCopyright");
        int year = 1900 + DateUtil.getYear(new Date());
        add(MsoyUI.createHTML(CMe.msgs.landingCopyright(""+year), "inline"));
        add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        add(MsoyUI.createExternalAnchor(
            "http://www.threerings.net", CMe.msgs.landingAbout()));
        add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        add(MsoyUI.createExternalAnchor(
            "http://wiki.whirled.com/Terms_of_Service", CMe.msgs.landingTerms()));
        add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        add(MsoyUI.createExternalAnchor(
            "http://www.threerings.net/about/privacy.html", CMe.msgs.landingPrivacy()));
        add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        add(Link.create(CMe.msgs.landingHelp(), Page.HELP, ""));
    }
}
