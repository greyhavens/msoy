//
// $Id$

package client.people;

import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.util.InfoCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

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
