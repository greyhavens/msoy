package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    /** Identifies stock thumbnail images. */
    public static const THUMBNAIL :String = "thumbnails";

    /** Identifies stock furniture visualizations. */
    public static const FURNI :String = "furni";

    public function StaticMediaDesc (type :String = null, itemType :int = 0)
    {
        _type = type;
        _itemType = itemType;
        mimeType = IMAGE_PNG;
    }

    // from MediaDesc
    override public function getMediaPath () :String
    {
        return "/media/static/" + _type + "/" +
            Item.getTypeName(_itemType).toLowerCase() + ".png";
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(_type);
        out.writeByte(_itemType);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _type = (ins.readField(String) as String);
        _itemType = ins.readByte();
        mimeType = IMAGE_PNG;
    }

    protected var _type :String;
    protected var _itemType :int;
}
}
