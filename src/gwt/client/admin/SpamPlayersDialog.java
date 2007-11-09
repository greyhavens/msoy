//
// $Id$

package client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import client.util.BorderedDialog;
import client.util.MsoyUI;
import client.util.PromptPopup;

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

        contents.setWidget(0, 0, new Label(CAdmin.msgs.spamIntro()));
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

        _footer.add(new Button(CAdmin.msgs.spamSend(), new ClickListener() {
            public void onClick (Widget sender) {
                spamPlayers(_subject.getText().trim(), _body.getText().trim(), false);
            }
        }));
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

    protected void spamPlayers (final String subject, final String body, boolean confirmed)
    {
        if (!confirmed) {
            new PromptPopup(CAdmin.msgs.spamConfirm()) {
                public void onAffirmative () {
                    spamPlayers(subject, body, true);
                }
            }.setContext("\"" + subject + "\"").prompt();
            return;
        }

        CAdmin.adminsvc.spamPlayers(CAdmin.ident, subject, body, new AsyncCallback() {
            public void onSuccess (Object result) {
                int[] counts = (int[])result;
                MsoyUI.info(CAdmin.msgs.spamSent(Integer.toString(counts[0]),
                                                 Integer.toString(counts[1]),
                                                 Integer.toString(counts[2])));
                hide();
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CAdmin.serverError(cause));
            }
        });
    }

    protected TextBox _subject;
    protected TextArea _body;
}
