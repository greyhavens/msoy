//
// $Id$

package client.people;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.FlashClients;
import client.util.Link;

/**
 * Displays a generic "invite your friends to Whirled" interface.
 */
public class WhirledInvitePanel extends InvitePanel
{
    public WhirledInvitePanel (boolean justRegistered)
    {
        String introMsg = justRegistered ?
            _msgs.newbInviteIntro(""+CoinAwards.INVITED_FRIEND_JOINED) : _msgs.inviteIntro();

        // TODO: if (justRegistered) need step 4 image
        add(new TongueBox(null, makeHeader("/images/people/share_header.png", introMsg)));
        add(new TongueBox(_msgs.inviteEmail(), new WhirledInviteEmailListPanel()));
        add(WidgetUtil.makeShim(20, 20));
        String shareURL = ShareUtil.getAffiliateLandingURL(Pages.LANDING);
        add(new TongueBox(_msgs.inviteIM(), new IMPanel(shareURL)));

        if (justRegistered) {
            HorizontalPanel done = new HorizontalPanel();
            done.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            done.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            done.setWidth("100%");
            String tip, button;
            if (FlashClients.clientExists()) {
                done.add(MsoyUI.createLabel(_msgs.inviteCloseTip(), null));
                done.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.inviteClose(),
                                             new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        CShell.frame.closeContent();
                    }
                }));
            } else {
                done.add(MsoyUI.createLabel(_msgs.inviteDoneTip(), null));
                done.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.inviteNext(),
                                             Link.createListener(Pages.WORLD, "h")));
            }
            add(WidgetUtil.makeShim(20, 20));
            add(new TongueBox(_msgs.inviteDone(), done));
        }
    }
}
