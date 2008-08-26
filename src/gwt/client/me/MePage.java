//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;
import client.util.FlashClients;
import client.util.Link;

public class MePage extends Page
{
    @Override // from Page
    public void onPageLoad ()
    {
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals("account")) {
            setContent(_msgs.titleAccount(), new EditAccountPanel());

        } else if (action.equals("passport")) {
            // guests should never get a link to a passport page that will use the default, so 0
            // is fine (it'll through an internal error on the server)
            int defaultId = CMe.creds == null ? 0 : CMe.creds.name.getMemberId();
            setContent(_msgs.titlePassport(), new PassportPanel(args.get(1, defaultId)));

        } else if (DeploymentConfig.devDeployment && action.equals("passportimagetest")) {
            setContent(_msgs.titlePassportTest(), new PassportImageTestPanel());

        } else if (!CMe.isGuest()) {
            setContent(new MyWhirled());
            FlashClients.tutorialEvent("myWhirledVisited");

        } else {
            Link.go(null, ""); // redirect to landing page
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ME;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
