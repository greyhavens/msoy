//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY main page.
 */
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

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        _entryCounter++;

        // cancel our refresher interval as we'll restart it if needed below
        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return;
        }

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CWorld.ident == null) {
            setContent(MsoyUI.createLabel(CWorld.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        try {
            String action = args.get(0, "s1");
            if (action.startsWith("s")) {
                String sceneId = action.substring(1);
                if (args.getArgCount() <= 1) {
                    WorldClient.displayFlash("sceneId=" + sceneId);
                } else {
                    // if we have sNN-extra-args we want the close button to use just "sNN"
                    WorldClient.displayFlash("sceneId=" + sceneId + "&page=" +
                                             Args.compose(args.splice(1)), "s" + sceneId);
                }

            } else if (action.startsWith("g")) {
                // go to a specific group's scene group
                WorldClient.displayFlash("groupHome=" + action.substring(1));

            } else if (action.startsWith("m")) {
                // go to a specific member's home
                WorldClient.displayFlash("memberHome=" + action.substring(1));

            } else if (action.startsWith("c")) {
                // join a group chat
                WorldClient.displayFlash("groupChat=" + action.substring(1));

            } else if (action.startsWith("p")) {
                // display popular places by request
                displayHotSpots(_entryCounter);

            } else {
                MsoyUI.error(CWorld.msgs.unknownLocation());
            }

        } catch (NumberFormatException e) {
            MsoyUI.error(CWorld.msgs.unknownLocation());
        }
    }

    // @Override // from Page
    public void onPageUnload ()
    {
        super.onPageUnload();

        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }
    }

    // @Override // from Page
    protected void didLogoff ()
    {
        // head to Whirledwide
        Application.go(Page.WHIRLED, "whirledwide");
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWorld.msgs = (WorldMessages)GWT.create(WorldMessages.class);
    }

    protected void displayHotSpots (final int requestEntryCount)
    {
        CWorld.membersvc.serializePopularPlaces(CWorld.ident, 20, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    hotSpots((String) result);
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
    }

    protected void hotSpots (String hotSpots)
    {
        setPageTitle(CWorld.msgs.hotSpotsTitle());
        setFlashContent(FlashClients.createPopularPlacesDefinition(hotSpots));
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "world";
    }

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    /** Handles periodic refresh of the popular places view. */
    protected Timer _refresher;

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;
}
