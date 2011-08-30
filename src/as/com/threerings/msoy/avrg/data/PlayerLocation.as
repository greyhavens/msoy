//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.StringUtil;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Helps an AVRG keep track of which scene a player is in.
 */
public class PlayerLocation
    implements DSet_Entry
 {
     /** The member id of the player in question. */
     public var playerId :int;

     /** The id of the scene currently occupied by this member. */
     public var sceneId :int;

     // from interface DSet_Entry
     public function getKey () :Object
     {
         return playerId;
     }

     // from Object
     public function toString () :String
     {
         return StringUtil.simpleToString(this);
     }

     // from interface Streamable
     public function readObject (ins :ObjectInputStream) :void
     {
         playerId = ins.readInt();
         sceneId = ins.readInt();
     }

     // from interface Streamable
     public function writeObject (out :ObjectOutputStream) :void
     {
         out.writeInt(playerId);
         out.writeInt(sceneId);
     }
 }
}
