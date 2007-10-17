//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.person.data.MailFolder;

import client.msgs.MsgsEntryPoint;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we have no creds, just display a message saying login
        if (CMail.ident == null) {
            setContent(MsoyUI.createLabel(CMail.msgs.logon(), "infoLabel"));
            _mainView = null;
            return;
        }

        // initialize the application, if necessary
        if (_mainView == null) {
            _mainView = new MailApplication();
        }
        // make sure we're displaying the application
        if (!_mainView.isAttached()) {
            setPageTitle(CMail.msgs.mailTitle());
            setContent(_mainView);
        }

        // display the requested folder, header and/or message
        int folderId = args.get(1, MailFolder.INBOX_FOLDER_ID);
        int headerOffset = args.get(2, 0);
        int messageId = args.get(3, -1);
        _mainView.show(folderId, headerOffset, messageId);
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "mail";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMail.msgs = (MailMessages)GWT.create(MailMessages.class);
    }

    protected MailApplication _mainView;
}
