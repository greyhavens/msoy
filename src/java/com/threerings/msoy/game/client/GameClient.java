//
// $Id$

package com.threerings.msoy.game.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.media.FrameManager;
import com.threerings.media.image.ImageManager;
import com.threerings.media.sound.JavaSoundPlayer;
import com.threerings.media.sound.SoundPlayer;
import com.threerings.media.tile.TileManager;

import com.threerings.resource.ResourceManager;
import com.threerings.util.IdleTracker;
import com.threerings.util.KeyDispatcher;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.parlor.client.ParlorDirector;

import com.threerings.toybox.client.ToyBoxDirector;

import com.whirled.util.WhirledContext;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.game.data.GameCredentials;

import static com.threerings.msoy.Log.log;

/**
 * Provides the necessary framework and classloading for Java games.
 */
public class GameClient
{
    /**
     * Initializes a new client and provides it with a frame in which to display everything.
     */
    public void init (JApplet applet, FrameManager fmgr)
        throws Exception
    {
        // create our context
        _ctx = createContextImpl();

        // create the directors/managers/etc. provided by the context
        createContextServices();

        // keep these for later
        _applet = applet;
        _fmgr = fmgr;
        _keydisp = new KeyDispatcher(_fmgr.getManagedRoot().getWindow());

        // stuff our top-level pane into the top-level of our shell
        _applet.setContentPane(_root);

        // display a "loading..." panel until our first placeview is set
        JPanel loading = GroupLayout.makeVBox();
        loading.setBackground(Color.white);
        loading.add(new JLabel("Loading..."));
        setMainPanel(loading);

        // start our idle tracker
        IdleTracker idler = new IdleTracker(ChatCodes.DEFAULT_IDLE_TIME, LOGOFF_DELAY) {
            protected long getTimeStamp () {
                return _fmgr.getTimeStamp();
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
                    BodyService bsvc = _ctx.getClient().requireService(BodyService.class);
                    bsvc.setIdle(_ctx.getClient(), isIdle);
                }
            }
            protected void abandonedShip () {
                if (_client.isLoggedOn()) {
                    _client.logoff(true);
                }
            }
        };
        idler.start(null, _fmgr.getManagedRoot().getWindow(), _ctx.getClient().getRunQueue());
    }

    public void start (String authtoken, int gameId, final int gameOid)
    {
        if (gameId == -1) {
            log.warning("Missing required parameters [game_id=" + gameId + "].");
            // TODO: display some error to the user
            return;
        }

        GameCredentials creds = new GameCredentials();
        creds.sessionToken = authtoken;
        _ctx.getClient().setCredentials(creds);
        _ctx.getClient().setVersion(DeploymentConfig.version);
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
        log.info("Logging on [creds=" + creds + ", version=" + DeploymentConfig.version + "]...");
        _ctx.getClient().logon();
    }

    /**
     * Returns a reference to the context in effect for this client. This reference is valid for
     * the lifetime of the application.
     */
    public WhirledContext getContext ()
    {
        return _ctx;
    }

    /**
     * Sets the main user interface panel.
     */
    public void setMainPanel (JComponent panel)
    {
        // remove the old panel
        _root.removeAll();
	// add the new one
	_root.add(panel, BorderLayout.CENTER);
        // swing doesn't properly repaint after adding/removing children
        _root.revalidate();
        _root.repaint();
    }

    /**
     * Creates the {@link WhirledContext} implementation that will be passed around to all of the
     * client code. Derived classes may wish to override this and create some extended context
     * implementation.
     */
    protected WhirledContext createContextImpl ()
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
        _client = new Client(null, RunQueue.AWT);

        // create our managers
        _rsrcmgr = new ResourceManager("rsrc");
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _imgmgr = new ImageManager(_rsrcmgr, _applet);
        _tilemgr = new TileManager(_imgmgr);
        _sndmgr = new JavaSoundPlayer(_rsrcmgr);

        // and our directors
        _locdir = new LocationDirector(_ctx) {
            public boolean moveBack () {
                try {
                    _applet.getAppletContext().showDocument(new URL("javascript:back()"));
                } catch (Exception e) {
                    log.warning("Failed to move back: " + e);
                }
                return false;
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

    /**
     * The context implementation. This provides access to all of the objects and services that are
     * needed by the operating client.
     */
    protected class GameContextImpl extends WhirledContext
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
            setMainPanel((JComponent)view);
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
            return _fmgr;
        }

        public KeyDispatcher getKeyDispatcher () {
            return _keydisp;
        }

        public ImageManager getImageManager () {
            return _imgmgr;
        }

        public TileManager getTileManager () {
            return _tilemgr;
        }

        public SoundPlayer getSoundManager () {
            return _sndmgr;
        }
    }

    protected WhirledContext _ctx;
    protected JApplet _applet;
    protected FrameManager _fmgr;
    protected JPanel _root = new JPanel(new BorderLayout()); // TODO?
    protected Config _config = new Config("toybox");

    protected Client _client;
    protected ResourceManager _rsrcmgr;
    protected MessageManager _msgmgr;
    protected KeyDispatcher _keydisp;

    protected ImageManager _imgmgr;
    protected TileManager _tilemgr;
    protected SoundPlayer _sndmgr;

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
