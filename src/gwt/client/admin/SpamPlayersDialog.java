//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

import client.shell.ShellMessages;
import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.NumberTextBox;
import client.util.RowPanel;
import client.util.ServiceUtil;

/**
 * Sends an "announcement message" to all registered players (who have not opted out of
 * announcements).
 */
public class SpamPlayersDialog extends BorderedDialog
{
    public SpamPlayersDialog ()
    {
        setHeaderTitle(CAdmin.msgs.spamTitle());

        SmartTable contents = new SmartTable("spamPlayers", 0, 5);

        contents.setText(0, 0, CAdmin.msgs.spamIntro(), 2, null);
        contents.getFlexCellFormatter().setWidth(0, 0, "500px");

        contents.setText(1, 0, CAdmin.msgs.spamSubject());
        contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setVisibleLength(50);
        _subject.setMaxLength(80);

        contents.addWidget(_body = new TextArea(), 2, null);
        _body.setCharacterWidth(60);
        _body.setVisibleLines(20);

        RowPanel niggles = new RowPanel();
        niggles.add(new Label(CAdmin.msgs.spamNiggles()));
        niggles.add(_startId = new NumberTextBox(false, 8));
        _startId.setText("0");
        niggles.add(_endId = new NumberTextBox(false, 8));
        _endId.setText("0");
        contents.addWidget(niggles, 2, null);
        setContents(contents);

        Button spam = new Button(CAdmin.msgs.spamSend());
        new ClickCallback<int[]>(spam, CAdmin.msgs.spamConfirm()) {
            public boolean callService () {
                String subject = _subject.getText().trim();
                String body = _body.getText().trim();
                int sid = _startId.getValue().intValue(), eid = _endId.getValue().intValue();
                _adminsvc.spamPlayers(CAdmin.ident, subject, body, sid, eid, this);
                MsoyUI.info(CAdmin.msgs.spammingPleaseWait());
                return true;
            }
            public boolean gotResult (int[] counts) {
                MsoyUI.info(CAdmin.msgs.spamSent(Integer.toString(counts[0]),
                                                 Integer.toString(counts[1]),
                                                 Integer.toString(counts[2])));
                hide();
                return false;
            }
        };
        addButton(spam);
        addButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    protected TextBox _subject;
    protected TextArea _body;
    protected NumberTextBox _startId, _endId;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
