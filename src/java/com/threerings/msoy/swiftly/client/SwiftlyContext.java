//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

/**
 * Provides necessary services, and juicy goodness.
 */
public class SwiftlyContext
    implements CrowdContext
{
    public SwiftlyContext (SwiftlyApplication app)
    {
        _client = new Client(null, RunQueue.AWT);
        _locdtr = new LocationDirector(this);
        _occdtr = new OccupantDirector(this);
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _chatdtr = new ChatDirector(this, _msgmgr, "chat");
        _translator = new MessageBundleTranslator(_msgmgr);
        _app = app;
    }

    // from CrowdContext
    public Config getConfig ()
    {
        return _config;
    }

    // from CrowdContext
    public Client getClient ()
    {
        return _client;
    }

    // from CrowdContext
    public DObjectManager getDObjectManager ()
    {
        return _client.getDObjectManager();
    }

    // from CrowdContext
    public LocationDirector getLocationDirector ()
    {
        return _locdtr;
    }

    // from CrowdContext
    public OccupantDirector getOccupantDirector ()
    {
        return _occdtr;
    }

    // from CrowdContext
    public ChatDirector getChatDirector ()
    {
        return _chatdtr;
    }

    // from CrowdContext
    public void setPlaceView (PlaceView view)
    {
        // nada
    }

    // from CrowdContext
    public void clearPlaceView (PlaceView view)
    {
        // nada
    }

    /**
     * Return the SwiftlyApplication associated with this SwiftlyContext.
     */
    public SwiftlyApplication getApplication ()
    {
        return _app;
    }

    /**
     * Get the Translator which uses data from this SwiftlyContext to perform translations.
     */
    public Translator getTranslator ()
    {
        return _translator;
    }

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    private static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";

    private final Config _config = new Config("swiftly");
    private final MessageManager _msgmgr;
    private final Translator _translator;

    private final Client _client;
    private final LocationDirector _locdtr;
    private final OccupantDirector _occdtr;
    private final ChatDirector _chatdtr;
    private final SwiftlyApplication _app;
}
