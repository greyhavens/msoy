//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.WebCreds;

import client.msgs.MsgsEntryPoint;
import client.shell.MsoyEntryPoint;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // if we have no creds, just display a message saying login
        if (CMail.creds == null) {
            setContent(MsoyUI.createLabel(CMail.msgs.logon(), "infoLabel"));
            return;
        }

        // initialize the application, if necessary
        if (_mainView == null) {
            _mainView = new MailApplication();
        }
        // make sure we're displaying the application
        if (!_mainView.isAttached()) {
            setContent(_mainView);
        }

        // set defaults to use in liu of sane URL contents
        int folderId = MailFolder.INBOX_FOLDER_ID;
        int headerOffset = 0;
        int messageId = -1;

        if (token != null && token.length() > 0) {
            try {
                String[] bits = token.substring(1).split("\\.");
                folderId = Integer.parseInt(bits[0]);
                if (bits.length > 1) {
                    headerOffset = Integer.parseInt(bits[1]);
                    if (bits.length > 2) {
                        messageId = Integer.parseInt(bits[2]);
                    }
                }
            } catch (Exception e) {
                // just use the defaults
            }
        }
        // finally update the application view
        _mainView.show(folderId, headerOffset, messageId);
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "mail";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMail.msgs = (MailMessages)GWT.create(MailMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        onHistoryChanged(History.getToken());
        return header;
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        _mainView = null;
        onHistoryChanged(null);
    }

    protected MailApplication _mainView;
}
