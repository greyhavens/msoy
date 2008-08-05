//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import client.shell.ShellMessages;

/**
 * Displays a popup for complaining about a message.
 */
public abstract class ComplainPopup extends BorderedDialog
    implements AsyncCallback<Void>
{
    public ComplainPopup ()
    {
        setHeaderTitle(_cmsgs.complainHeader());

        VerticalPanel vbox = new VerticalPanel();
        vbox.setStyleName("complainPopup");
        vbox.setSpacing(5);
        vbox.add(MsoyUI.createLabel(_cmsgs.complainMessage(), null));
        vbox.add(MsoyUI.createLabel(_cmsgs.complainDesc(), null));
        vbox.add(_description = MsoyUI.createTextBox("", 512, 50));
        setContents(vbox);

        ClickListener sendComplain = new ClickListener() {
            public void onClick (Widget sender) {
                sendComplain();
            }
        };
        _description.addKeyboardListener(new EnterClickAdapter(sendComplain));

        addButton(new Button(_cmsgs.send(), sendComplain));
        addButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    /**
     * Returns true if you want to hide the popup when the service call is made and ignore
     * any return values.
     */
    public boolean hideOnSend ()
    {
        return true;
    }

    // from interface AsyncCallback
    public void onSuccess (Void result)
    {
        if (!hideOnSend()) {
            hide();
        }
    }

    // from interface AsyncCallback
    public void onFailure (Throwable cause)
    {
        // nothing by default
    }

    /**
     * Derived classes should override this method and submit the complaint.
     */
    protected abstract boolean callService ();

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        _description.setFocus(true);
    }

    protected void sendComplain ()
    {
        if ("".equals(_description.getText())) {
            MsoyUI.info(_cmsgs.complainNeedDescription());
            return;
        }
        if (callService() && hideOnSend()) {
            hide();
        }
    }

    protected TextBox _description;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
