//
// $Id$

package client.landing;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a copyright message and links to TOS, privacy policy, etc..
 */
public class LandingCopyright extends FlowPanel
{
    public static FlowPanel addFinePrint (FlowPanel panel)
    {
        int year = 1900 + DateUtil.getYear(new Date());
        panel.add(MsoyUI.createHTML(_msgs.landingCopyright(""+year), "inline"));
        panel.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        panel.add(MsoyUI.createExternalAnchor(
                "http://wiki.whirled.com/Terms_of_Service", _msgs.landingTerms()));
        panel.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        panel.add(MsoyUI.createExternalAnchor(
                "http://www.threerings.net/about/privacy.html", _msgs.landingPrivacy()));
        panel.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        panel.add(Link.create(_msgs.landingHelp(), Pages.HELP, ""));
        return panel;
    }

    public LandingCopyright ()
    {
        setStyleName("LandingCopyright");
        addFinePrint(this);
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
