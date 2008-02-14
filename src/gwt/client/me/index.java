//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.client.WorldServiceAsync;
import com.threerings.msoy.web.data.Invitation;

import client.msgs.MsgsEntryPoint;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.FlashClients;
import client.util.MsoyCallback;
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
        String action = args.get(0, "");

        if (action.equals("help")) {
            setContent(new HelpPanel());

        } else if (action.equals("account")) {
            setContent(new EditAccountPanel());

        } else if (action.equals("i") && CMe.getMemberId() == 0) {
            // only load their invitation and redirect to the main page if they're not logged in
            String inviteId = args.get(1, "");
            if (Application.activeInvite != null &&
                Application.activeInvite.inviteId.equals(inviteId)) {
                Application.go(Page.ME, "");
            } else {
                CMe.membersvc.getInvitation(inviteId, true, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        Application.activeInvite = (Invitation)result;
                        Application.go(Page.ME, "");
                    }
                });
            }

        } else if (CMe.getMemberId() != 0) {
            setContent(new MyWhirled(createPopulationDisplay()));
            FlashClients.tutorialEvent("myWhirledVisited");

        } else {
            displayWhat();
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return ME;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CMe.worldsvc = (WorldServiceAsync)GWT.create(WorldService.class);
        ((ServiceDefTarget)CMe.worldsvc).setServiceEntryPoint("/worldsvc");

        // load up our translation dictionaries
        CMe.msgs = (MeMessages)GWT.create(MeMessages.class);
    }

    protected void displayWhat ()
    {
        Frame.closeClient(false); // no client on the main guest landing page
        setContent(new WhatIsTheWhirled(), false);
    }

    protected PopulationDisplay createPopulationDisplay ()
    {
        return new PopulationDisplay() {
            public void displayPopulation (int population) {
                // This is a hack to get the population into the usual tabs spot...
                VerticalPanel container = new VerticalPanel();
                container.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
                container.add(WidgetUtil.makeShim(5, 3));
                Label popLabel = new Label(CMe.msgs.populationDisplay("" + population));
                popLabel.setStyleName("PopulationDisplay");
                container.add(popLabel);
                setPageTabs(container);
            }
        };
    }
}
