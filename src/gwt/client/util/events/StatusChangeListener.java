//
// $Id$

package client.util.events;

public interface StatusChangeListener extends FlashEventListener
{
    public void statusChanged (StatusChangeEvent event);
}
