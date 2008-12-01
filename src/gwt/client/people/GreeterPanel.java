//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;
import com.threerings.msoy.web.gwt.WebMemberService.FriendsResult;

import client.ui.HeaderBox;
import client.util.ServiceBackedDataModel;
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

        MemberList greeters = new MemberList(_msgs.noGreeters(), "GreetersPanel");
        add(new HeaderBox(_msgs.greetersListTitle(), greeters));
        greeters.setModel(new GreeterDataModel(), 0);
    }

    /**
     * Data model to fetch the greeters one page at a time.
     */
    protected static class GreeterDataModel
        extends ServiceBackedDataModel<MemberCard, FriendsResult>
    {
        @Override
        protected void callFetchService (
            int start, int count, boolean needCount, AsyncCallback<FriendsResult> callback)
        {
            // we ignore needCount because the server always sets totalCount
            _membersvc.loadGreeters(start, count, callback);
        }

        @Override
        protected int getCount (FriendsResult result)
        {
            return result.totalCount;
        }

        @Override
        protected List<MemberCard> getRows (FriendsResult result)
        {
            return result.friendsAndGreeters;
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
