//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a generic "invite your friends to Whirled" interface.
 */
public class WhirledInvitePanel extends InvitePanel
{
    public WhirledInvitePanel (boolean justRegistered)
    {
        String introMsg = justRegistered ? _msgs.newbInviteIntro() : _msgs.inviteIntro();
        SmartTable intro = new SmartTable();
        intro.setWidget(0, 0, new Image("/images/people/share_header.png"));
        intro.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        intro.setWidget(0, 2, MsoyUI.createHTML(introMsg, null));
        add(intro);

        // buttons to invoke the various ways to invite
        addMethodButton("Email", new InviteMethodCreator() {
            public Widget create () {
                return new WhirledInviteEmailListPanel();
            }
        });
        addMethodButton("IM", new InviteMethodCreator() {
            public Widget create () {
                return new IMPanel(ShareUtil.getAffiliateLandingUrl(Pages.LANDING));
            }
        });
        if (justRegistered) {
            _buttons.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, "Done!",
                                             Link.createListener(Pages.WORLD, "h")));
        }
        addMethodButtons();
    }
}
