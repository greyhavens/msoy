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
import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;
import client.util.FlashClients;

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
    protected void onPageLoad ()
    {
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
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        clientLogon(creds.getMemberId(), creds.token);
        updateInterface(History.getToken());
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        clientLogoff();
        updateInterface(History.getToken());
    }

    protected void updateInterface (String token)
    {
        RootPanel.get("content").clear();
        _entryCounter ++;

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return;
        }

        try {
            if (token.startsWith("s")) {
                // go to a specific scene
                world("sceneId=" + id(token, 1));

            } else if (token.startsWith("g")) {
                // go to a specific group's scene group
                world("groupHome=" + id(token, 1));

            } else if (token.startsWith("m")) {
                // go to a specific member's home
                world("memberHome=" + id(token, 1));

            } else if (token.startsWith("ng")) {
                // go to the neighborhood around the specified group
                displayNeighborhood(_entryCounter, id(token, 2), true);

            } else if (token.startsWith("nm")) {
                // go to the neighborhood around the specified member
                displayNeighborhood(_entryCounter, id(token, 2), false);

            } else if (token.startsWith("p")) {
                // display popular places by request
                displayHotSpots(_entryCounter);

            } else if (CWorld.creds != null) {
                // we're logged in, go to our home
                world(null);

            } else {
                // we're not logged in, show popular places
                displayHotSpots(_entryCounter);
            }

        } catch (NumberFormatException e) {
            // if all else fails, display popular places
            displayHotSpots(_entryCounter);
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
        setContent(_client = FlashClients.createNeighborhood(hood, "100%", "550"));
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
