//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.DeploymentConfig;

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
    public void onHistoryChanged (String token)
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
            if (token.startsWith("s")) {
                // go to a specific scene - don't parse out an int here, because a gameId may
                // be encoded in the token
                WorldClient.displayFlash("sceneId=" + token.substring(1));

            } else if (token.startsWith("g")) {
                // go to a specific group's scene group
                WorldClient.displayFlash("groupHome=" + id(token, 1));

            } else if (token.startsWith("m")) {
                // go to a specific member's home
                WorldClient.displayFlash("memberHome=" + id(token, 1));

            } else if (token.startsWith("l")) {
                // go to a specific member's home
                WorldClient.displayFlash("location=" + id(token, 1));

            } else if (token.startsWith("p") || CWorld.ident != null) {
                // display popular places by request or if we're logged in
                displayHotSpots(_entryCounter);

            } else {
                // we're not logged in, show popular places
                displayHotSpots(_entryCounter);
            }

        } catch (NumberFormatException e) {
            // if all else fails, display popular places
            displayHotSpots(_entryCounter);
        }
    }

    // @Override // from Page
    public void onPageLoad ()
    {
        super.onPageLoad();
        configureHistoryCallback(this);
    }

    // @Override // from Page
    public void onPageUnload ()
    {
        super.onPageUnload();

        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }
        
        configureHistoryCallback(null);
    }

    /**
     * Exposed to Flash via {@link configureHistoryCallback}, so that Flash clients can add
     * new tokens to the history stack.
     */
    public void addHistoryToken (String token)
    {
        // note: the following operation will trigger onHistoryChanged
        History.newItem(token);
    }
        
    // @Override // from Page
    protected void didLogoff ()
    {
        onHistoryChanged("p");
    }

    // @Override // from Page
    protected boolean needsHeaderClient ()
    {
        String token = getPageArgs();
        return (token.startsWith("ng") || token.startsWith("nm") || token.startsWith("p") ||
                CWorld.ident == null);
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
                    HTML widget = hotSpots((String) result);
                    scheduleReload();
                    widget.addMouseListener(new MouseListenerAdapter() {
                        public void onMouseMove(Widget sender, int x, int y) {
                            scheduleReload();
                        }
                    });
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
    }

    protected void scheduleReload ()
    {
        if (_refresher == null) {
            _refresher = new Timer() {
                public void run() {
                    onHistoryChanged(getPageArgs());
                }
            };
        }
        _refresher.schedule(NEIGHBORHOOD_REFRESH_TIME * 1000);
    }

    protected HTML hotSpots (String hotSpots)
    {
        setPageTitle(CWorld.msgs.hotSpotsTitle());
        HTML content = setFlashContent(FlashClients.createPopularPlacesDefinition(hotSpots));
        setCloseButton();
        return content;
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
     * Configures foreign function interface for access from Flash.
     */
    protected static native void configureHistoryCallback (index idx) /*-{
        if (idx != null) {
            $wnd.updateSceneHistory = function (token) {
                idx.@client.world.index::addHistoryToken(Ljava/lang/String;)(token);
            };
        } else {
            $wnd.updateSceneHistory = null;
        }
    }-*/;

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    /** Handles periodic refresh of the popular places view. */
    protected Timer _refresher;

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;
}
