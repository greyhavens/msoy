//
// $Id$

package client.util.events;

public interface StatusChangeListener extends FlashEventListener
{
    void statusChanged (StatusChangeEvent event);
}
