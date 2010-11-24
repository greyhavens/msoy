//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.ui.MsoyUI;
import client.util.InfoCallback;

public class MuteList extends MemberList
{
    public MuteList (int memberId)
    {
        super(_msgs.mutelistEmpty(), "MutelistPanel");
        _memberId = memberId;
    }

    @Override
    protected Widget createWidget (MemberCard member)
    {
        return new MutelistMemberWidget(member);
    }

    protected void removeMute (final MemberCard card)
    {
        _membersvc.setMuted(_memberId, card.name.getId(), false, new InfoCallback<Void>() {
            public void onSuccess (Void nothing) {
                removeItem(card);
            }
        });
    }

    protected class MutelistMemberWidget extends MemberWidget
    {
        public MutelistMemberWidget (MemberCard card)
        {
            super(card);
        }

        @Override
        protected void addExtras (SmartTable extras, final MemberCard card)
        {
            super.addExtras(extras, card);

            int row = extras.getRowCount();

            // add a way to have them removed
            ClickHandler onClick = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    removeMute(card);
                }
            };
            extras.setWidget(row, 0,
                MsoyUI.createActionImage("/images/profile/remove.png", onClick));
            extras.setWidget(row++, 1,
                MsoyUI.createActionLabel(_msgs.mutelistRemove(), onClick));
        }
    }

    protected int _memberId;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
