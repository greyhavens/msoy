package com.threerings.msoy.item.web {

import mx.utils.URLUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    public function StaticMediaDesc (mimeType :int, itemType :int, mediaType :String)
    {
        super(null, mimeType);
        _itemType = itemType;
        _mediaType = mediaType;
    }

    // from MediaDesc
    override public function getMediaPath () :String
    {
        return URLUtil.getFullURL(DeploymentConfig.mediaURL,
            "/media/static/" + Item.getTypeName(_itemType) + "/" + _mediaType + ".png");
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(_itemType);
        out.writeField(_mediaType);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _itemType = ins.readByte();
        _mediaType = (ins.readField(String) as String);
    }

    protected var _itemType :int;
    protected var _mediaType :String;
}
}
