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

    public function StaticMediaDesc (type :String, item :String)
    {
        _type = type;
        _item = item;
        mimeType = IMAGE_PNG;
    }

    // from MediaDesc
    override public function getMediaPath () :String
    {
        return "/media/static/" + _type + "/" + _item.toLowerCase() + ".png";
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(_type);
        out.writeField(_item);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        _type = (ins.readField(String) as String);
        _item = (ins.readField(String) as String);
        mimeType = IMAGE_PNG;
    }

    protected var _type :String;
    protected var _item :String;
}
}
