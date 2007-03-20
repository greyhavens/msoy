//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.Page;
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
    public void onHistoryChanged (String token)
    {
        updateInterface(token);
    }

    // @Override // from Page
    protected boolean needsHeaderClient ()
    {
        String token = getPageArgs();
        return (token.startsWith("ng") || token.startsWith("nm") || token.startsWith("p") ||
                CWorld.creds == null);
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWorld.msgs = (WorldMessages)GWT.create(WorldMessages.class);
    }

    // @Override // from Page
    protected boolean didLogon (WebCreds creds)
    {
        // super.didLogon(creds);
        clientLogon(creds.getMemberId(), creds.token);
        return updateInterface(getPageArgs());
    }

    // @Override // from Page
    protected void didLogoff ()
    {
        // super.didLogoff();
        clientLogoff();
        updateInterface(getPageArgs());
    }

    protected boolean updateInterface (String token)
    {
        _entryCounter++;

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return false;
        }

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CWorld.creds == null) {
            needPopupHack = false;
            setContent(MsoyUI.createLabel(CWorld.cmsgs.noGuests(), "infoLabel"));
            return false;
        }

        needPopupHack = true;
        try {
            if (token.startsWith("s")) {
                // go to a specific scene
                world("sceneId=" + id(token, 1));
                return false;

            } else if (token.startsWith("g")) {
                // go to a specific group's scene group
                world("groupHome=" + id(token, 1));
                return false;

            } else if (token.startsWith("m")) {
                // go to a specific member's home
                world("memberHome=" + id(token, 1));
                return false;

            } else if (token.startsWith("l")) {
                // go to a specific member's home
                world("location=" + id(token, 1));
                return false;

            } else if (token.startsWith("ng")) {
                // go to the neighborhood around the specified group
                displayNeighborhood(_entryCounter, id(token, 2), true);
                return true;

            } else if (token.startsWith("nm")) {
                // go to the neighborhood around the specified member
                displayNeighborhood(_entryCounter, id(token, 2), false);
                return true;

            } else if (token.startsWith("p")) {
                // display popular places by request
                displayHotSpots(_entryCounter);
                return true;

            } else if (CWorld.creds != null) {
                // we're logged in, go to our home
                world(null);
                return false;

            } else {
                // we're not logged in, show popular places
                displayHotSpots(_entryCounter);
                return true;
            }

        } catch (NumberFormatException e) {
            // if all else fails, display popular places
            displayHotSpots(_entryCounter);
            return true;
        }
    }

    protected void displayNeighborhood (final int requestEntryCount, int entityId, boolean isGroup)
    {
        CWorld.membersvc.serializeNeighborhood(
            CWorld.creds, entityId, isGroup, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    neighborhood((String) result);
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
        scheduleReload();
    }

    protected void displayHotSpots (final int requestEntryCount)
    {
        CWorld.membersvc.serializePopularPlaces(CWorld.creds, 20, new AsyncCallback() {
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
        scheduleReload();
    }

    protected void scheduleReload ()
    {
        new Timer() {
            public void run() {
                updateInterface(getPageArgs());
            }
        }.schedule(NEIGHBORHOOD_REFRESH_TIME * 1000);
    }

    protected void neighborhood (String hood)
    {
        if (_mode == WORLD_MODE) {
            clientLogoff();
            // TODO: start up the header client
        }
        _mode = HOOD_MODE;
        if (hood == null) {
            setContent(new Label(CWorld.msgs.noSuchMember()));
        } else {
            setContent(_client = FlashClients.createNeighborhood(hood));
        }
    }

    protected void hotSpots (String hotSpots)
    {
        if (_mode == WORLD_MODE) {
            clientLogoff();
            // TODO: start up the header client
        }
        _mode = POPULAR_MODE;
        setContent(_client = FlashClients.createPopularPlaces(hotSpots));
    }

    protected void world (String flashVar)
    {
        if (_mode == WORLD_MODE) {
            CWorld.log("Going " + flashVar);
            if (clientGo(flashVar)) {
                return;
            }
            // otherwise fall through and reload the page
        }

        CWorld.log("Full reloading " + flashVar);
        _mode = WORLD_MODE;
        if (CWorld.creds != null) {
            flashVar = "token=" + CWorld.creds.token + "&" + flashVar;
        }
        setContent(_client = FlashClients.createWorldClient(flashVar));
    }

    protected int id (String token, int index)
    {
        return Integer.valueOf(token.substring(index)).intValue();
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "world";
    }

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientLogon(memberId, token);
        }
    }-*/;

    /**
     * Tells the World client to go to a particular location.
     */
    protected static native boolean clientGo (String where) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientGo(where);
            return true;
        }
        return false;
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogoff () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientLogoff();
        }
    }-*/;

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    /** The display mode we're in. */
    protected int _mode = -1;

    protected HTML _client;

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;

    protected static final int WORLD_MODE = 0;
    protected static final int HOOD_MODE = 1;
    protected static final int POPULAR_MODE = 2;
}
