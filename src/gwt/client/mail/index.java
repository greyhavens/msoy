//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;

import client.msgs.MsgsEntryPoint;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
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
            return;
        }

        String action = args.get(0, "");
        if (action.equals("c")) {
            setContent(CMail.msgs.mailTitle(), new ConvoPanel(_model, args.get(1, 0)));
        } else if (action.equals("w")) {
            setContent(CMail.msgs.mailTitle(), new ComposePanel(args.get(1, 0)));
        } else {
            setContent(CMail.msgs.mailTitle(), new MailPanel(_model, args.get(0, 0)));
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return MAIL;
    }

    // @Override // from Page
    protected String getTabPageId ()
    {
        return ME;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMail.msgs = (MailMessages)GWT.create(MailMessages.class);
    }

    protected ConvosModel _model = new ConvosModel();
}
