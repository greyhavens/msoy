//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

/**
 * Notifies when a scene is added or removed.
 */
public class SceneBookmarkEvent extends FlashEvent
{
    /** The action dispatched when a scene is added: defined in BaseClient.as. */
    public static final int SCENEBOOKMARK_ADDED = 1;

    /** The action dispatched when a scene is removed: defined in BaseClient.as. */
    public static final int SCENEBOOKMARK_REMOVED = 2;

    /** The name of this event type: defined in BaseClient.as. */
    public static final String NAME = "sceneBookmark";

    @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    public SceneBookmarkEvent ()
    {
    }

    public SceneBookmarkEvent (int action, String name, int sceneId)
    {
        _action = action;
        _sceneName = name;
        _sceneId = sceneId;
    }

    @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args)
    {
        _action = FlashClients.getIntElement(args, 0);
        _sceneName = FlashClients.getStringElement(args, 1);
        _sceneId = FlashClients.getIntElement(args, 2);
    }

    @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof SceneBookmarkListener) {
            switch (_action) {
            case SCENEBOOKMARK_ADDED:
                ((SceneBookmarkListener) listener).sceneAdded(this);
                break;
            case SCENEBOOKMARK_REMOVED:
                ((SceneBookmarkListener) listener).sceneRemoved(this);
                break;
            }
        }
    }

    public int getAction ()
    {
        return _action;
    }

    public String getSceneName ()
    {
        return _sceneName;
    }

    public int getSceneId ()
    {
        return _sceneId;
    }

    protected int _action;
    protected String _sceneName;
    protected int _sceneId;
}
