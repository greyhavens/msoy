//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.util.InfoCallback;

/**
 * Click listener that removes a friend.
 */
public class FriendRemover extends PromptPopup
{
    /**
     */
    public FriendRemover (final MemberName target, final Command success)
    {
        super(_msgs.mlRemoveConfirm(target.toString()), new Command () {
            public void execute () {
                _membersvc.removeFriend(target.getMemberId(), new InfoCallback<Void>() {
                    public void onSuccess (Void result) {
                        MsoyUI.info(_msgs.mlRemoved(target.toString()));
                        if (success != null) {
                            success.execute();
                        }
                    }
                });
            }
        });
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
