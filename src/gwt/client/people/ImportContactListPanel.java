//
// $Id$

package client.people;

import java.util.Collection;

import client.ui.Checklist;
import client.ui.MsoyUI;
import client.ui.StretchPanel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.web.gwt.EmailContact;

public class ImportContactListPanel extends FlowPanel
{
    public ImportContactListPanel (Collection<EmailContact> contacts)
    {
        setStyleName("importContactList-wrapper");

        // Central contents panel
        StretchPanel container = new StretchPanel("importContactList");
        VerticalPanel contents = new VerticalPanel();
        contents.add(MsoyUI.createLabel("Invite Friends from Your Address Book",
            "importContactList-header"));
        contents.add(MsoyUI.createHTML("Select the people from your address book that you would like " +
        		"to send this invitation to.  We'll give you <b>1000 coins</b> for each person " +
        		"that joins through your invite, and we'll add them to your friends list as soon " +
        		"as they sign up.", "importContactList-description"));
        Widget invite = MsoyUI.createButton("shortThin", "Invite", new ClickListener() {
            public void onClick (Widget sender) {
                // TODO
                Window.alert("invite");
            }
        });
        contents.add(invite);
        contents.setCellHorizontalAlignment(invite, HasHorizontalAlignment.ALIGN_CENTER);

        // List of contacts
        Checklist<EmailContact> contactList = new Checklist<EmailContact>(contacts) {
            @Override protected Widget createWidgetFor (EmailContact item) {
                SmartTable contactWidget = new SmartTable(0, 0);
                contactWidget.setText(0, 0, item.name, 1, "importContactList-listName");
                contactWidget.setText(0, 1, item.email, 1, "importContactList-listEmail");
                return contactWidget;
            }
        };
        contents.add(contactList);

        // Second button at the bottom of the list.
        invite = MsoyUI.createButton("shortThin", "Invite", new ClickListener() {
            public void onClick (Widget sender) {
                // TODO
                Window.alert("invite 2");
            }
        });
        contents.add(invite);
        contents.setCellHorizontalAlignment(invite, HasHorizontalAlignment.ALIGN_CENTER);

        container.setContent(contents);
        add(container);
    }
}
