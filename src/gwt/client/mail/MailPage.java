//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;

public class MailPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we have no creds, just display a message saying logon
        if (CMail.isGuest()) {
            setContent(MsoyUI.createLabel(_msgs.logon(), "infoLabel"));
            return;
        }

        String action = args.get(0, "");
        if (action.equals("c")) {
            setContent(_msgs.mailTitle(), new ConvoPanel(_model, args.get(1, 0)));

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
            setContent(_msgs.mailTitle(), compose);

        } else {
            setContent(_msgs.mailTitle(), new MailPanel(_model, args.get(0, 0)));
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.MAIL;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CMail.msgs = (MailMessages)GWT.create(MailMessages.class);
    }

    protected ConvosModel _model = new ConvosModel();

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
}
