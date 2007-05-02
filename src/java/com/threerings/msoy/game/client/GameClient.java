//
// $Id$

package com.threerings.msoy.game.client;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JPanel;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.util.IdleTracker;
import com.threerings.util.KeyDispatcher;
import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.media.FrameManager;
import com.threerings.resource.ResourceManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.ezgame.data.GameDefinition;
import com.threerings.parlor.client.ParlorDirector;

import com.threerings.toybox.client.ToyBoxDirector;
import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.web.client.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Provides the necessary framework and classloading for Java games.
 */
public class GameClient
    implements RunQueue
{
    /**
     * Initializes a new client and provides it with a frame in which to display everything.
     */
    public void init (GameApplet shell)
        throws Exception
    {
        // create our context
        _ctx = createContextImpl();

        // create the directors/managers/etc. provided by the context
        createContextServices();

        // keep this for later
        _shell = shell;
        _keydisp = new KeyDispatcher(_shell.getWindow());

        // stuff our top-level pane into the top-level of our shell
        _shell.setContentPane(_root);

        // start our idle tracker
        IdleTracker idler = new IdleTracker(ChatCodes.DEFAULT_IDLE_TIME, LOGOFF_DELAY) {
            protected long getTimeStamp () {
                return _shell.getFrameManager().getTimeStamp();
            }
            protected void idledIn () {
                updateIdle(false);
            }
            protected void idledOut () {
                updateIdle(true);
            }
            protected void updateIdle (boolean isIdle) {
                if (_ctx.getClient().isLoggedOn()) {
                    log.info("Setting idle " + isIdle + ".");
                    BodyService bsvc = (BodyService)
                        _ctx.getClient().requireService(BodyService.class);
                    bsvc.setIdle(_ctx.getClient(), isIdle);
                }
            }
            protected void abandonedShip () {
                if (_client.isLoggedOn()) {
                    _client.logoff(true);
                }
            }
        };
        idler.start(null, _shell.getWindow(), _ctx.getClient().getRunQueue());
    }

    public void start (String authtoken, int gameId, final int gameOid)
    {
        if (StringUtil.isBlank(authtoken) || gameId == -1) {
            log.warning("Missing required parameters [authtoken=" + authtoken +
                        ", game_id=" + gameId + "].");
            // TODO: display some error to the user
            return;
        }

        MsoyCredentials creds = new MsoyCredentials();
        creds.sessionToken = authtoken;
        _ctx.getClient().setCredentials(creds);
        _ctx.getClient().setVersion(String.valueOf(DeploymentConfig.version));
        _ctx.getClient().addClientObserver(new ClientAdapter() {
            public void clientDidLogon (Client client) {
                if (gameOid != -1) {
                    _ctx.getLocationDirector().moveTo(gameOid);
                } // else TODO
            }
            public void clientFailedToLogon (Client client, Exception cause) {
                log.warning("Failed to logon to server: " + cause);
                // TODO: display message to user
            }
        });
        _ctx.getClient().logon();
    }

    /**
     * Returns a reference to the context in effect for this client. This reference is valid for
     * the lifetime of the application.
     */
    public ToyBoxContext getContext ()
    {
        return _ctx;
    }

    /**
     * Sets the main user interface panel.
     */
    public void setMainPanel (JPanel panel)
    {
        // remove the old panel
        _root.removeAll();
	// add the new one
	_root.add(panel, BorderLayout.CENTER);
        // swing doesn't properly repaint after adding/removing children
        _root.revalidate();
        _root.repaint();
    }

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable run)
    {
        // queue it on up on the awt thread
        EventQueue.invokeLater(run);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }

    /**
     * Creates the {@link ToyBoxContext} implementation that will be passed around to all of the
     * client code. Derived classes may wish to override this and create some extended context
     * implementation.
     */
    protected ToyBoxContext createContextImpl ()
    {
        return new GameContextImpl();
    }

    /**
     * Creates and initializes the various services that are provided by the context. Derived
     * classes that provide an extended context should override this method and create their own
     * extended services. They should be sure to call <code>super.createContextServices</code>.
     */
    protected void createContextServices ()
        throws Exception
    {
        // create the handles on our various services
        _client = new Client(null, this);

        // create our managers and directors
        _rsrcmgr = new ResourceManager("rsrc");
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _locdir = new LocationDirector(_ctx) {
            public boolean moveBack () {
                log.info("TODO!");
                return false;
            }

            protected PlaceController createController (PlaceConfig config) {
                if (config instanceof MsoyGameConfig) {
                    return createGameController((MsoyGameConfig)config);
                } else {
                    return super.createController(config);
                }
            }
        };
        _occdir = new OccupantDirector(_ctx);
        _chatdir = new ChatDirector(_ctx, _msgmgr, MsoyCodes.CHAT_MSGS);
        _pardtr = new ParlorDirector(_ctx);
    }

//     /** Makes our client controller visible to the dispatch system. */
//     protected class RootPanel extends JPanel
//         implements ControllerProvider
//     {
//         public RootPanel () {
//             super(new BorderLayout());
//         }

//         public Controller getController () {
//             return _cctrl;
//         }
//     }

    protected PlaceController createGameController (MsoyGameConfig config)
    {
        String path = config.getGameDefinition().getMediaPath(config.getGameId());
        ClassLoader loader;
        try {
            loader = new URLClassLoader(new URL[] { new URL(path) }, getClass().getClassLoader());
        } catch (Exception e) {
            log.warning("Failed to create game class loader [path=" + path + ", error=" + e + "].");
            return null;
        }

        String ccls = config.getGameDefinition().controller;
        PlaceController ctrl;
        try {
            ctrl = (PlaceController)Class.forName(ccls, true, loader).newInstance();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create controller [class=" + ccls + "].", e);
            return null;
        }

        // configure the resource manager to load media from the game's class loader
        _rsrcmgr.setClassLoader(loader);

        // configure the distributed object system to load classes from the game's class loader
        _ctx.getClient().setClassLoader(loader);

        // configure our message manager with this class loader so that we can obtain translation
        // resources from the game message bundles
        _ctx.getMessageManager().setClassLoader(loader);

        return ctrl;
    }

    /**
     * The context implementation. This provides access to all of the objects and services that are
     * needed by the operating client.
     */
    protected class GameContextImpl extends ToyBoxContext
    {
        /**
         * Apparently the default constructor has default access, rather than protected access,
         * even though this class is declared to be protected. Why, I don't know, but we need to be
         * able to extend this class elsewhere, so we need this.
         */
        protected GameContextImpl () {
        }

        public Client getClient () {
            return _client;
        }

        public DObjectManager getDObjectManager () {
            return _client.getDObjectManager();
        }

        public Config getConfig () {
            return _config;
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public LocationDirector getLocationDirector () {
            return _locdir;
        }

        public OccupantDirector getOccupantDirector () {
            return _occdir;
        }

        public ChatDirector getChatDirector () {
            return _chatdir;
        }

        public ParlorDirector getParlorDirector () {
            return _pardtr;
        }

        public void setPlaceView (PlaceView view) {
            setMainPanel((JPanel)view);
        }

        public void clearPlaceView (PlaceView view) {
            // we'll just let the next place view replace our old one
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public ToyBoxDirector getToyBoxDirector () {
            throw new RuntimeException("ToyBoxDirector is not supported in Whirled.");
        }

        public FrameManager getFrameManager () {
            return _shell.getFrameManager();
        }

        public KeyDispatcher getKeyDispatcher () {
            return _keydisp;
        }
    }

    protected ToyBoxContext _ctx;
    protected GameApplet _shell;
    protected JPanel _root = new JPanel(new BorderLayout()); // TODO?
    protected Config _config = new Config("toybox");

    protected Client _client;
    protected ResourceManager _rsrcmgr;
    protected MessageManager _msgmgr;
    protected KeyDispatcher _keydisp;

    protected LocationDirector _locdir;
    protected OccupantDirector _occdir;
    protected ChatDirector _chatdir;
    protected ParlorDirector _pardtr;

    /** The prefix prepended to localization bundle names before looking them up in the
     * classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";

    /** The time in milliseconds after which we log off an idle user. */
    protected static final long LOGOFF_DELAY = 8L * 60L * 1000L;
}
