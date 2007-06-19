//
// $Id$

package client.util.events;

public interface LevelsListener extends FlashEventListener
{
    public void levelUpdated (LevelUpdateEvent event);
}
