//
// $Id$

package com.threerings.msoy.world.server;

/**
 * Takes care of loading, unloading and handling of Pets.
 */
public class PetManager
{
    /**
     * Loads up all pets that are "let out" in the specified room. Any pets that live in this room
     * but are currently being walked will not be added to the room (they are already extant in the
     * world).
     */
    public void loadRoomPets (int roomOid)
    {
    }
}
