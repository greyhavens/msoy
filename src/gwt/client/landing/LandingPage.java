//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.shell.Page;
import client.ui.NoNavPanel;
import client.util.Link;
import client.util.events.ThemeChangeEvent;

/**
 * The main entry point for the landing page(s).
 */
public class LandingPage extends Page
{
    public static final String OLD_BLUE_LANDING = "bluelanding";
    public static final String PLAY_BLUE_LANDING = "playbluelanding";
    public static final String NEW_MONSTER_LANDING = "monsterlanding";
    public static final String DJ_LANDING = "dj";
    public static final String DJ_DARK_LANDING = "djdark";
    public static final String DJ_LIGHT_LANDING = "djlight";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        // landing page with an introduction to Whirled for players (register-focused blue)
        if (action.equals(OLD_BLUE_LANDING)) {
            setContent(_msgs.titleLanding(), NoNavPanel.makeBlue(new LandingPanel(false)));

        // landing page with an introduction to Whirled for players (play-as-guest-focused blue)
        } else if (action.equals(PLAY_BLUE_LANDING)) {
            setContent(_msgs.titleLanding(), NoNavPanel.makeBlue(new LandingPanel(true)));

        // landing page with an introduction to Whirled for developers (monster avenue)
        } else if (action.equals(NEW_MONSTER_LANDING)) {
            setContent(_msgs.titleLanding(), new LandingMonsterPanel());

        } else if (action.equals(DJ_DARK_LANDING)) {
            setContent(_msgs.djTitleLanding(), new LandingDjPanel(false));

        } else if (action.equals(DJ_LIGHT_LANDING)) {
            setContent(_msgs.djTitleLanding(), new LandingDjPanel(true));

        } else if (CShell.getClientMode() == ClientMode.WHIRLED_DJ || action.equals(DJ_LANDING)) {
            // Split test between the light and dark landing page
            _membersvc.getABTestGroup(CShell.frame.getVisitorInfo(),
                "2012 01 DJ landings, dark (1) light (2)", true, new AsyncCallback<Integer>() {
                public void onSuccess (Integer group) {
                    gotABGroup(group);
                }
                public void onFailure (Throwable cause) {
                    gotABGroup(-1);
                }
                protected void gotABGroup (int group) {
                    setContent(_msgs.djTitleLanding(), new LandingDjPanel(group != 1));
                }
            });

        } else {
            if (action.equals("theme")) {
                int themeId = args.get(1, 0);
                if (themeId > 0) {
                    CShell.frame.dispatchEvent(new ThemeChangeEvent(themeId));
                }
            }

            _membersvc.getABTestGroup(CShell.frame.getVisitorInfo(),
                "2010 05 register (1) room (2)", true, new AsyncCallback<Integer>() {
                public void onSuccess (Integer group) {
                    gotABGroup(group);
                }
                public void onFailure (Throwable cause) {
                    gotABGroup(-1);
                }
                protected void gotABGroup (int group) {
                    if (group == 1) {
                        Link.go(Pages.LANDING, OLD_BLUE_LANDING);
                    } else {
                        Link.go(Pages.WORLD, "places");
                    }
                }
            });
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.LANDING;
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);

    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
