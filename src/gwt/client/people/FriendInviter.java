//
// $Id$

package client.people;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

/**
 * Click listener that invites or adds a friend when clicked. First asks the server if the target
 * is an automatic friender (socialite), and if so adds the friend immediately. Otherwise, pops
 * up an invite conversation dialog.
 * 
 * TODO: take a widget parameter and use ClickCallback (this requires significant work in
 * callers). For now, just prevent reentrancy and don't reimplement widget-less ClickCallback.
 * 
 * TODO: we actually need to hide the control if the friending takes place automatically...
 * or maybe leaving it disabled would work
 */
public class FriendInviter
    implements ClickListener
{
    /**
     * Creates a new inviter that will add the supplied target as a friend or dispatch an email
     * request to do so. The caller id is used to track the event.
     */
    public FriendInviter (MemberName target, String callerId)
    {
        _target = target;
        _callerId = callerId;
    }

    // from ClickListener
    public void onClick (Widget sender)
    {
        // guard against reentry
        if (_clicked) {
            return;
        }

        try {
            _clicked = true;
            doClick();
        } catch (ServiceException sex) {
        }
    }
        
    protected void doClick ()
        throws ServiceException
    {
        _membersvc.isAutomaticFriender(_target.getMemberId(), new AsyncCallback<Boolean>() {
            public void onFailure (Throwable caught) {
                _clicked = false;
            }

            public void onSuccess (Boolean automatic) {
                _clicked = false;
                if (automatic) {
                    doAutomaticFriending();
                } else {
                    new InviteFriendPopup(_target).show();
                }
            }
        });
    }
    
    protected void doAutomaticFriending ()
    {
        _membersvc.addFriend(_target.getMemberId(), new AsyncCallback<Void>() {
            public void onFailure (Throwable caught) {
                MsoyUI.error(CShell.serverError(caught));
            }

            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.ifriendAdded());
                _membersvc.trackClientAction(
                    CShell.visitor, "autoFriended" + _callerId, null, new NoopAsyncCallback());
            }
        });
    }

    protected boolean _clicked;
    protected MemberName _target;
    protected String _callerId;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
