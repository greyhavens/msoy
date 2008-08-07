//
// $Id$

package com.threerings.msoy.room.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.msoy.item.data.all.Pet;

/**
 * Defines services provided for Pets in the virtual world.
 */
public interface PetService extends InvocationService
{
    /**
     * Calls the specified pet. If the pet is not loaded, it will be resolved and placed in the
     * room occupied by the caller. If the pet is loaded, it will simply move to the room occupied
     * by the caller. In both cases, the pet will automatically be placed in follow mode.
     */
    void callPet (Client client, int petId, ConfirmListener listener);

    /**
     * Issues an order to the specified pet. Different orders have different access control
     * restrictions, see the {@link Pet} order constants for details.
     */
    void orderPet (Client client, int petId, int order, ConfirmListener listener);

    /**
     * Sends a chat message from the pet with specified id and name.
     */
    public void sendChat (
        Client client, int bodyOid, int sceneId, String message, ConfirmListener listener);
}
