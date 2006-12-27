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
import client.shell.ShellContext;
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
        updateInterface(History.getToken());
    }

    // @Override // from MsoyEntryPoint
    protected ShellContext createContext ()
    {
        return _ctx = new WorldContext();
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        updateInterface(History.getToken());
        clientLogon(creds.memberId, creds.token);
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
                world("sceneId=" + id(token, 1));
                return;
            }
            if (token.startsWith("g")) {
                world("groupHome=" + id(token, 1));
                return;
            }
            if (token.startsWith("m")) {
                world("memberHome=" + id(token, 1));
                return;
            }
            if (token.startsWith("ng")) {
                final int requestEntryCount = _entryCounter;
                _ctx.membersvc.serializeNeighborhood(
                    _ctx.creds, id(token, 2), true, new AsyncCallback() {
                        public void onSuccess (Object result) {
                            if (requestEntryCount == _entryCounter) {
                                neighborhood((String) result);
                            }
                        }
                        public void onFailure (Throwable caught) {
                            if (requestEntryCount == _entryCounter) {
                            }
                        }
                    });
                return;
            }
            if (token.startsWith("nm")) {
                final int requestEntryCount = _entryCounter;
                _ctx.membersvc.serializeNeighborhood(
                    _ctx.creds, id(token, 2), false, new AsyncCallback() {
                        public void onSuccess (Object result) {
                            if (requestEntryCount == _entryCounter) {
                                neighborhood((String) result);
                            }
                        }
                        public void onFailure (Throwable caught) {
                            if (requestEntryCount == _entryCounter) {
                            }
                        }
                    });
                return;
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        // if we got crud in the URL, let's rewrite it (and trigger another call to ourselves)
        if (token.length() > 0) {
            History.newItem("");
            return;
        }
        if (_ctx.creds != null) {
            world(null);
            return;
        }
        final int requestEntryCount = _entryCounter;
        _ctx.membersvc.serializePopularPlaces(_ctx.creds, 20, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    hotSpots((String) result);
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label("Failed to fetch poular places: " + caught.getMessage()));
                }
            }
        });
    }

    protected void neighborhood (String hood)
    {
        if (_client != null) {
            clientLogoff();
        }
        setContent(_client = FlashClients.createNeighborhood(hood));
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

    protected WorldContext _ctx;

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    protected HTML _client;
}
