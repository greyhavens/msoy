//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.ui.MsoyUI;

public class index extends Page
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

    @Override // from Page
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
            ComposePanel compose = new ComposePanel();
            for (int ii = 1; ii < args.getArgCount(); ii++) {
                String extra = args.get(ii, "");
                if (extra.equals("m")) {
                    compose.setRecipientId(args.get(++ii, 0));
                } else if (extra.equals("g")) {
                    compose.setGroupInviteId(args.get(++ii, 0));
                } else if (extra.equals("i")) {
                    compose.setGiftItem((byte)args.get(++ii, 0), args.get(++ii, 0));
                }
            }
            setContent(CMail.msgs.mailTitle(), compose);

        } else {
            setContent(CMail.msgs.mailTitle(), new MailPanel(_model, args.get(0, 0)));
        }
    }

    @Override
    public String getPageId ()
    {
        return MAIL;
    }

    @Override // from Page
    protected String getTabPageId ()
    {
        return ME;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMail.msgs = (MailMessages)GWT.create(MailMessages.class);
    }

    protected ConvosModel _model = new ConvosModel();
}
