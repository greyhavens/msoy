//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class PetEvent extends FlashEvent
{
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
     * returns true if this pet was added to the room, false if removed. 
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
