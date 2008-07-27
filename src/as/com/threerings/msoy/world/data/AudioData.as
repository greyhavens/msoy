//
// $Id$

package com.threerings.msoy.world.data {

import flash.utils.ByteArray;

import com.threerings.util.Cloneable;
import com.threerings.util.Equalable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains information on background audio in a scene.
 */
public class AudioData
    implements Cloneable, Streamable, Equalable
{
    /** Identifies the id of the item that was used to create this. */
    public var itemId :int;

    /** Media contained in the audio item. */
    public var media :MediaDesc;

    /** Audio volume. */
    public var volume :Number;

    /**
     * Constructor, populates the audio data object with default values.
     */
    public function AudioData ()
    {
        itemId = 0;
        volume = 1.0;
        media = INVALID_MEDIA;
    }

    /**
     * Helper function: specifies that this decor data structure has already been
     * populated from a Decor item object.
     */
    public function isInitialized () :Boolean
    {
        return itemId != 0;
    }
    
    // documentation inherited from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        // compare audio item and volume
        if (other is AudioData) {
            var data :AudioData = other as AudioData;
            return data.itemId == this.itemId && data.volume == this.volume;
        }
        return false;
    }

    public function toString () :String
    {
        return "Audio[itemId=" + itemId + ", media=" + media + ", volume=" + volume + "]";
    }

    // documentation inherited from interface Cloneable
    public function clone () :Object
    {
        var data :AudioData = new AudioData();
        // perform a shallow copy of value attributes
        data.itemId = itemId;   
        data.volume = volume;
        data.media = media;
        return data;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(itemId);
        out.writeObject(media);
        out.writeFloat(volume);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        media = ins.readObject() as MediaDesc;
        volume = ins.readFloat();
    }

    /** Media descriptor that represents invalid background audio (temporary). */
    protected static const INVALID_MEDIA :MediaDesc =
        new DefaultItemMediaDesc(MediaDesc.AUDIO_MPEG, Item.AUDIO, Item.FURNI_MEDIA);

}
}
