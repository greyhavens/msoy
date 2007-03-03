//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY main page.
 */
public class index extends MsoyEntryPoint
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
        updateInterface(token);
    }

    // @Override // from MsoyEntryPoint
    protected boolean needsHeaderClient ()
    {
        String token = History.getToken();
        return (token.startsWith("ng") || token.startsWith("nm") || token.startsWith("p") ||
                CWorld.creds == null);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        needPopupHack = true;
        History.addHistoryListener(this);
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWorld.msgs = (WorldMessages)GWT.create(WorldMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        clientLogon(creds.getMemberId(), creds.token);
        return updateInterface(History.getToken());
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        clientLogoff();
        updateInterface(History.getToken());
    }

    protected boolean updateInterface (String token)
    {
        RootPanel.get("content").clear();
        _entryCounter++;

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return false;
        }

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CWorld.creds == null) {
            setContent(MsoyUI.createLabel(CWorld.cmsgs.noGuests(), "infoLabel"));
            return false;
        }

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
    }

    protected void neighborhood (String hood)
    {
        if (_client != null) {
            clientLogoff();
        }
        if (hood == null) {
            setContent(new Label(CWorld.msgs.noSuchMember()));
        } else {
            setContent(_client = FlashClients.createNeighborhood(hood));
        }
    }

    protected void hotSpots (String hotSpots)
    {
        if (_client != null) {
            clientLogoff();
        }
        setContent(_client = FlashClients.createPopularPlaces(hotSpots));
    }

    protected void world (String flashVar)
    {
        if (CWorld.creds != null) {
            flashVar = "token=" + CWorld.creds.token + "&" + flashVar;
        }
        setContent(_client = FlashClients.createWorldClient(flashVar));
    }

    protected int id (String token, int index)
    {
        return Integer.valueOf(token.substring(index)).intValue();
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "world";
    }

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        try {
            if ($doc.asclient) {
                $doc.asclient.clientLogon(memberId, token);

            } else if ($wnd.asclient) {
                $wnd.asclient.clientLogon(memberId, token);
            }
        } catch (e) {
            // oh well
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogoff () /*-{
        try {
            if ($doc.asclient) {
                $doc.asclient.clientLogoff();
            } else if ($wnd.asclient) {
                $wnd.asclient.clientLogoff();
            }
        } catch (e) {
            // oh well
        }
    }-*/;

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    protected HTML _client;
}
