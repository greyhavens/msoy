//
// $Id$

package client.adminz;

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
import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.ServiceUtil;

/**
 * Sends an "announcement message" to all registered players (who have not opted out of
 * announcements).
 */
public class SpamPlayersDialog extends BorderedDialog
{
    public SpamPlayersDialog ()
    {
        setHeaderTitle(_msgs.spamTitle());

        SmartTable contents = new SmartTable("spamPlayers", 0, 5);

        contents.setText(0, 0, _msgs.spamIntro(), 2, null);
        contents.getFlexCellFormatter().setWidth(0, 0, "500px");

        contents.setText(1, 0, _msgs.spamSubject());
        contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setVisibleLength(50);
        _subject.setMaxLength(80);

        contents.addWidget(_body = new TextArea(), 2, null);
        _body.setCharacterWidth(60);
        _body.setVisibleLines(20);

        RowPanel niggles = new RowPanel();
        niggles.add(new Label(_msgs.spamNiggles()));
        niggles.add(_startId = new NumberTextBox(false, 8));
        _startId.setText("0");
        niggles.add(_endId = new NumberTextBox(false, 8));
        _endId.setText("0");
        contents.addWidget(niggles, 2, null);
        setContents(contents);

        Button spam = new Button(_msgs.spamSend());
        new ClickCallback<int[]>(spam, _msgs.spamConfirm()) {
            public boolean callService () {
                String subject = _subject.getText().trim();
                String body = _body.getText().trim();
                int sid = _startId.getValue().intValue(), eid = _endId.getValue().intValue();
                _adminsvc.spamPlayers(subject, body, sid, eid, this);
                MsoyUI.info(_msgs.spammingPleaseWait());
                return true;
            }
            public boolean gotResult (int[] counts) {
                MsoyUI.info(_msgs.spamSent(Integer.toString(counts[0]),
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

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
