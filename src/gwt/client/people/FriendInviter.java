//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;

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
    implements ClickHandler
{
    /**
     * Creates a new inviter that will add the supplied target as a friend or dispatch an email
     * request to do so. The caller id is used to track the event.
     */
    public FriendInviter (MemberName target, String callerId)
    {
        this(target, callerId, null);
    }

    /**
     * Creates a new inviter that will add the supplied target as a friend or dispatch an email
     * request to do so. The caller id is used to track the event.
     */
    public FriendInviter (MemberName target, String callerId, Command success)
    {
        this(target, callerId, success, null);
    }

    /**
     * Make it possible to force the bypass of the mail popup (or force it on)
     * if we happen to already know the greeter status of the target.
     */
    public FriendInviter (
        MemberName target, String callerId, Command success, Boolean forcedAutomatic)
    {
        _target = target;
        _callerId = callerId;
        _success = success;
        _forcedAutomatic = forcedAutomatic;
    }

    // from ClickHandler
    public void onClick (ClickEvent event)
    {
        if (!MsoyUI.requireRegistered()) {
            return; // permaguests can't make friends
        }
        // guard against reentry
        if (_clicked) {
            return;
        }
        _clicked = true;
        doClick();
    }

    protected void doClick ()
    {
        if (_forcedAutomatic != null) {
            doAction(_forcedAutomatic);
            return;
        }

        _membersvc.isAutomaticFriender(_target.getId(), new AsyncCallback<Boolean>() {
            public void onFailure (Throwable caught) {
                _clicked = false;
            }
            public void onSuccess (Boolean automatic) {
                _clicked = false;
                doAction(automatic);
            }
        });
    }

    protected void doAction (boolean automatic)
    {
        if (automatic) {
            doAutomaticFriending();
        } else {
            new InviteFriendPopup(_target, _success).show();
        }
    }

    protected void doAutomaticFriending ()
    {
        _membersvc.addFriend(_target.getId(), new AsyncCallback<Void>() {
            public void onFailure (Throwable caught) {
                MsoyUI.error(CShell.serverError(caught));
            }
            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.ifriendAdded());
                if (_success != null) {
                    _success.execute();
                }
            }
        });
    }

    protected boolean _clicked;
    protected MemberName _target;
    protected String _callerId;
    protected Command _success;
    protected Boolean _forcedAutomatic;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
