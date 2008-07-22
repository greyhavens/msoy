//
// $Id$

package client.util.events;

/**
 * Used to notify interested parties of changes to scene ownership.
 */
public interface SceneBookmarkListener extends FlashEventListener
{
    /** Called when a scene is added to the scenes list. */
    void sceneAdded (SceneBookmarkEvent event);

    /** Called when a scene is removed from the scenes list. */
    void sceneRemoved (SceneBookmarkEvent event);
}
