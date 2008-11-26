//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.ui.HeaderBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays all of the greeters online.
 */
public class GreeterPanel extends FlowPanel
{
    public GreeterPanel ()
    {
        setStyleName("greetersPanel");

        add(WidgetUtil.makeShim(10, 10));

        _membersvc.loadGreeters(
            new MsoyCallback<WebMemberService.FriendsResult>() {
                public void onSuccess (WebMemberService.FriendsResult result) {
                    gotGreeters(result);
                }
            });
    }

    protected void gotGreeters (WebMemberService.FriendsResult data)
    {
        if (data == null) {
            return;
        }

        MemberList greeters = new MemberList(_msgs.noGreetersOnline(), "GreetersPanel");
        add(new HeaderBox(_msgs.friendsWhoseGreeters(), greeters));
        greeters.setModel(new SimpleDataModel<MemberCard>(data.friendsAndGreeters), 0);
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
