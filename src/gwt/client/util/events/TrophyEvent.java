//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

/**
 * An event reported by the Flash client when the user received a trophy.
 */
public class TrophyEvent extends FlashEvent
{
    /** Implemented by entities which wish to listen for this event. */
    public static interface Listener extends FlashEventListener
    {
        /** Notifies the listener that the supplied even was received. */
        public void trophyEarned (TrophyEvent event);
    }

    /** The name of this event type: defined in GameLiaison.as. */
    public static final String NAME = "trophy";

    @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    public TrophyEvent ()
    {
    }

    /**
     * Returns the id of the game in which the trophy was earned.
     */
    public int getGameId ()
    {
        return _gameId;
    }

    /**
     * Returns the name of the game in which the trophy was earned.
     */
    public String getGame ()
    {
        return _game;
    }

    /**
     * Returns the name of the trophy was earned.
     */
    public String getTrophy ()
    {
        return _trophy;
    }

    /**
     * Returns the description of the trophy was earned.
     */
    public String getDescription ()
    {
        return _descrip;
    }

    /**
     * Returns the trophy media URL.
     */
    public String getMediaURL ()
    {
        return _mediaURL;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _gameId = JavaScriptUtil.getIntElement(args, 0);
        _game = JavaScriptUtil.getStringElement(args, 1);
        _trophy = JavaScriptUtil.getStringElement(args, 2);
        _descrip = JavaScriptUtil.getStringElement(args, 3);
        _mediaURL = JavaScriptUtil.getStringElement(args, 4);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _gameId);
        JavaScriptUtil.setStringElement(args, 1, _game);
        JavaScriptUtil.setStringElement(args, 2, _trophy);
        JavaScriptUtil.setStringElement(args, 3, _descrip);
        JavaScriptUtil.setStringElement(args, 4, _mediaURL);
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof Listener) {
            ((Listener) listener).trophyEarned(this);
        }
    }

    protected int _gameId;
    protected String _game;
    protected String _trophy;
    protected String _descrip;
    protected String _mediaURL;
}
