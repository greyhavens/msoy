//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class OptOutPanel extends SmartTable
{
    public OptOutPanel (String arg1, int memberId)
    {
        super("optout", 0, 10);

        // if this is a member invitation optout we'll get (inviteId, 0)
        if (memberId == 0) {
            _membersvc.getInvitation(arg1, false, new AsyncCallback<Invitation>() {
                public void onSuccess (Invitation invite) {
                    showInvite(invite, false);
                }
                public void onFailure (Throwable cause) {
                    setText(0, 0, CShell.serverError(cause));
                }
            });

        } else {
            setText(0, 0, _msgs.optOutOpting(), 1, "Header");
            // otherwise it's an announcement mailing and we get (memberId, hash)
            _membersvc.optOutAnnounce(memberId, arg1, new AsyncCallback<String>() {
                public void onSuccess (String result) {
                    setText(0, 0, _msgs.optOutOptedOut(result), 1, "Header");
                    setWidget(1, 0, MsoyUI.createHTML(_msgs.optOutReenable(), null));
                }
                public void onFailure (Throwable cause) {
                    setText(0, 0, CShell.serverError(cause), 1, "");
                }
            });
        }
    }

    public OptOutPanel (String gameInviteId)
    {
        super("optout", 0, 10);

        _membersvc.getGameInvitation(gameInviteId, new AsyncCallback<Invitation>() {
            public void onSuccess (Invitation invite) {
                showInvite(invite, true);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CShell.serverError(cause));
            }
        });
    }

    protected void showInvite (final Invitation invite, final boolean game)
    {
        int row = 0;
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        setText(row++, 0, _msgs.optOutIntro(invite.inviteeEmail));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Body");
        setText(row++, 0, _msgs.optOutBody1());

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(new Button(_msgs.optOutAccept(), new ClickListener() {
            public void onClick (Widget widget) {
                _membersvc.optOut(game, invite.inviteId, new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        clear();
                        setText(1, 0, _msgs.optOutSuccessful(invite.inviteeEmail));
                    }
                });
            }
        }));
        footer.add(WidgetUtil.makeShim(10, 10));
        footer.add(new Button(_msgs.optOutReject(), new ClickListener() {
            public void onClick (Widget widget) {
                Link.go(Pages.ME, "");
            }
        }));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, footer);
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
