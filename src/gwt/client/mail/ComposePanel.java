//
// $Id$

package client.mail;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.MemberCard;

import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Provides an interface for starting a new conversation with another member.
 */
public class ComposePanel extends FlowPanel
{
    public ComposePanel (int recipientId)
    {
        setStyleName("compose");
        addStyleName("pagedGrid"); // for our header and footer

        CMail.membersvc.getMemberCard(recipientId, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MemberCard)result);
            }
        });
    }

    protected void init (MemberCard recipient)
    {
        _recipient = recipient;

        SmartTable header = new SmartTable("Header", 0, 0);
        header.setWidth("100%");
        header.setHTML(0, 0, "&nbsp;", 1, "TopLeft");
        header.setText(0, 1, CMail.msgs.composeWriteTo(recipient.name.toString()), 1, "Middle");
        header.getFlexCellFormatter().addStyleName(0, 1, "WriteTo");
        header.setHTML(0, 2, "&nbsp;", 1, "TopRight");
        add(header);

        SmartTable contents = new SmartTable("Contents", 0, 5);
        contents.setWidget(0, 0, new ThumbBox(recipient.photo, null));
        contents.getFlexCellFormatter().setRowSpan(0, 0, 4);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        contents.setText(0, 1, CMail.msgs.composeTo(), 1, "Label");
        contents.setText(0, 2, recipient.name.toString());

        contents.setText(1, 0, CMail.msgs.composeSubject(), 1, "Label");
        contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setWidth("390px");

        contents.setText(2, 0, CMail.msgs.composeMessage(), 1, "Label");
        contents.getFlexCellFormatter().setVerticalAlignment(2, 0, HasAlignment.ALIGN_TOP);
        contents.setWidget(2, 1, _body = new TextArea());
        _body.setVisibleLines(10);
        _body.setWidth("390px");

        HorizontalPanel buttons = new HorizontalPanel();
        Button send = new Button(CMail.msgs.composeSend());
        buttons.add(send);
        new ClickCallback(send) {
            public boolean callService () {
                String subject = _subject.getText().trim();
                String body = _body.getText().trim();
                if (subject.length() == 0) {
                    MsoyUI.error(CMail.msgs.composeMissingSubject());
                    return false;
                }
                if (body.length() == 0) {
                    MsoyUI.error(CMail.msgs.composeMissingBody());
                    return false;
                }
                CMail.mailsvc.startConversation(
                    CMail.ident, _recipient.name.getMemberId(), subject, body, null, this);
                return true;
            }
            public boolean gotResult (Object result) {
                MsoyUI.info(CMail.msgs.composeSent(_recipient.name.toString()));
                History.back();
                return false;
            }
        };
        buttons.add(WidgetUtil.makeShim(10, 10));
        buttons.add(new Button(CMail.msgs.composeDiscard(), new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        }));
        contents.setWidget(3, 1, buttons);

        add(contents);

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        footer.setHTML(0, 1, "&nbsp;", 1, "Middle");
        footer.getFlexCellFormatter().addStyleName(0, 1, "Subject");
        footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
        add(footer);

        // start our focus in the subject field
        _subject.setFocus(true);
    }

    protected MemberCard _recipient;
    protected TextBox _subject;
    protected TextArea _body;
}
