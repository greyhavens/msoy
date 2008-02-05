//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.MsoyCallback;
import client.util.NumberTextBox;
import client.util.PromptPopup;
import client.util.RowPanel;

/**
 * Sends an "announcement message" to all registered players (who have not opted out of
 * announcements).
 */
public class SpamPlayersDialog extends BorderedDialog
{
    public SpamPlayersDialog ()
    {
        _header.add(createTitleLabel(CAdmin.msgs.spamTitle(), null));

        FlexTable contents = (FlexTable)_contents;
        contents.setStyleName("spamPlayers");

        contents.setText(0, 0, CAdmin.msgs.spamIntro());
        contents.getFlexCellFormatter().setColSpan(0, 0, 2);
        contents.getFlexCellFormatter().setWidth(0, 0, "500px");

        contents.setText(1, 0, CAdmin.msgs.spamSubject());
        contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setVisibleLength(50);
        _subject.setMaxLength(80);

        contents.setWidget(2, 0, _body = new TextArea());
        contents.getFlexCellFormatter().setColSpan(2, 0, 2);
        _body.setCharacterWidth(60);
        _body.setVisibleLines(20);

        RowPanel niggles = new RowPanel();
        niggles.add(new Label(CAdmin.msgs.spamNiggles()));
        niggles.add(_startId = new NumberTextBox(false, 8));
        _startId.setText("0");
        niggles.add(_endId = new NumberTextBox(false, 8));
        _endId.setText("0");
        contents.setWidget(3, 0, niggles);
        contents.getFlexCellFormatter().setColSpan(3, 0, 2);

        Button spam = new Button(CAdmin.msgs.spamSend());
        new ClickCallback(spam, CAdmin.msgs.spamConfirm()) {
            public boolean callService () {
                String subject = _subject.getText().trim();
                String body = _body.getText().trim();
                int sid = _startId.getValue().intValue(), eid = _endId.getValue().intValue();
                CAdmin.adminsvc.spamPlayers(CAdmin.ident, subject, body, sid, eid, this);
                MsoyUI.info(CAdmin.msgs.spammingPleaseWait());
                return true;
            }
            public boolean gotResult (Object result) {
                int[] counts = (int[])result;
                MsoyUI.info(CAdmin.msgs.spamSent(Integer.toString(counts[0]),
                                                 Integer.toString(counts[1]),
                                                 Integer.toString(counts[2])));
                hide();
                return false;
            }
        };
        _footer.add(spam);
        _footer.add(new Button(CAdmin.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        return new FlexTable();
    }

    protected TextBox _subject;
    protected TextArea _body;
    protected NumberTextBox _startId, _endId;
}
