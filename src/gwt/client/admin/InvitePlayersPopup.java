//
// $Id$

package client.admin;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;

/**
 * Displays an interface for inviting players (creating them an account and sending them an invite
 * email).
 */
public class InvitePlayersPopup extends BorderedDialog
    implements ClickListener
{
    public InvitePlayersPopup ()
    {
        FlexTable content = (FlexTable)_contents;
        content.setStyleName("invitePlayersPopup");
        content.getFlexCellFormatter().setStyleName(0, 0, "Tip");
        content.setText(0, 0, CAdmin.msgs.inviteTip());
        content.setWidget(1, 0, _addrs = new TextArea());
        _addrs.setCharacterWidth(50);
        _addrs.setVisibleLines(15);

        _header.add(new Label(CAdmin.msgs.inviteTitle()));
        _footer.add(_submit = new Button(CAdmin.msgs.inviteSubmit(), this));
        _footer.add(new Button(CAdmin.cmsgs.dismiss(), this));
    }

    // @Override // from PopupPanel
    public void show ()
    {
        super.show();
        _addrs.setFocus(true);
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        if (sender != _submit) {
            hide();
        }

        // no double clicky
        _submit.setEnabled(false);

        ArrayList list = new ArrayList();
        String addrs = _addrs.getText();
        int sidx = 0, nidx = 0;
        while ((nidx = addrs.indexOf("\n", nidx)) != -1) {
            String addr = addrs.substring(sidx, nidx).trim();
            if (addr.length() > 0) {
                list.add(addr);
            }
            sidx = nidx;
            nidx += 1;
        }
        String addr = addrs.substring(sidx).trim();
        if (addr.length() > 0) {
            list.add(addr);
        }

        // no ArrayList.toArray() in GWT, whee!
        _emails = new String[list.size()];
        for (int ii = 0; ii < _emails.length; ii++) {
            _emails[ii] = (String)list.get(ii);
        }

        CAdmin.adminsvc.registerAndInvite(CAdmin.ident, _emails, new AsyncCallback() {
            public void onSuccess (Object result) {
                String[] responses = (String[])result;
                String text = "";
                for (int ii = 0; ii < responses.length; ii++) {
                    if (text.length() > 0) {
                        text += "\n";
                    }
                    text += _emails[ii] + ": ";
                    if (responses[ii] == null) {
                        text += CAdmin.msgs.inviteSent();
                    } else {
                        text += CAdmin.serverError(responses[ii]);
                    }
                }
                _addrs.setText(text);
            }
            public void onFailure (Throwable cause) {
                _addrs.setText(CAdmin.serverError(cause));
            }
        });
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }

    protected TextArea _addrs;
    protected Button _submit;
    protected String[] _emails;
}
