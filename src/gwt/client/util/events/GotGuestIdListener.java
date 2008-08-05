//
// $Id$

package client.util.events;

/**
 * Dispatches a {@link GotGuestIdEvent} to interested parties.
 */
public interface GotGuestIdListener extends FlashEventListener
{
    void gotGuestId (GotGuestIdEvent event);
}
