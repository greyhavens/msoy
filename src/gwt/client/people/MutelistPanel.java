//
// $Id: ProfilePanel.java 15555 2009-03-19 23:45:27Z mdb $

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.HeaderBox;
import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

/**
 * Displays a member's mutelist.
 */
public class MutelistPanel extends FlowPanel
{
    public MutelistPanel (int memberId)
    {
        setStyleName("mutelistPanel");

        boolean self = (CShell.getMemberId() == memberId);
        String title = self ? _msgs.mutelistTitle()
                            : _msgs.mutelistOtherTitle(String.valueOf(memberId));
        MuteList mutes = new MuteList(memberId);
        add(new HeaderBox(title, mutes));
        mutes.setModel(new MutelistDataModel(memberId), 0);
    }

    protected static class MutelistDataModel
        extends PagedServiceDataModel<MemberCard, PagedResult<MemberCard>>
    {
        public MutelistDataModel (int memberId)
        {
            _memberId = memberId;
        }

        @Override
        protected void callFetchService (
            int start, int count, boolean needCount,
            AsyncCallback<PagedResult<MemberCard>> callback)
        {
            _membersvc.loadMutelist(_memberId, start, count, callback);
        }

        protected int _memberId;
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
