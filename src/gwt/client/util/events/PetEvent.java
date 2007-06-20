//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class PetEvent extends FlashEvent
{
    /** The name of this event type: defined in RoomView.as. */
    public static final String NAME = "pet";

    // @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        _added = FlashClients.getBooleanElement(args, 0);
        _petId = FlashClients.getIntElement(args, 1);
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof PetListener) {
            ((PetListener) listener).petUpdated(this);
        }
    }

    /**
     * Returns true if this pet was added to the room, false if it was removed. 
     */
    public boolean addedToRoom () 
    {
        return _added;
    }

    public int getPetId ()
    {
        return _petId;
    }

    protected int _petId;
    protected boolean _added;
}
