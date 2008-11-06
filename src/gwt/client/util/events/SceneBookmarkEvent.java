//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

/**
 * Notifies when a scene is added or removed.
 */
public class SceneBookmarkEvent extends FlashEvent
{
    /** The action dispatched when a scene is added: defined in MsoyClient.as. */
    public static final int SCENEBOOKMARK_ADDED = 1;

    /** The action dispatched when a scene is removed: defined in MsoyClient.as. */
    public static final int SCENEBOOKMARK_REMOVED = 2;

    /** The name of this event type: defined in MsoyClient.as. */
    public static final String NAME = "sceneBookmark";

    public SceneBookmarkEvent ()
    {
    }

    public SceneBookmarkEvent (int action, String name, int sceneId)
    {
        _action = action;
        _sceneName = name;
        _sceneId = sceneId;
    }

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _action = JavaScriptUtil.getIntElement(args, 0);
        _sceneName = JavaScriptUtil.getStringElement(args, 1);
        _sceneId = JavaScriptUtil.getIntElement(args, 2);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _action);
        JavaScriptUtil.setStringElement(args, 1, _sceneName);
        JavaScriptUtil.setIntElement(args, 2, _sceneId);
    }

    @Override // from FlashEvent
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
