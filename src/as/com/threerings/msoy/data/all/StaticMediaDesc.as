//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.data.all.MediaMimeTypes;

import com.threerings.util.Log;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDescImpl
{
    public function StaticMediaDesc (mimeType :int = 0, itemType :String = null,
                                     mediaType :String = null, constraint :int = NOT_CONSTRAINED)
    {
        super(mimeType, constraint);
        _itemType = itemType;
        _mediaType = mediaType;
    }

    // from MediaDesc
    override public function getMediaPath () :String
    {
        Log.getLog(this).warning(
            "Unfolding StaticMediaDesc", "mimeType", _mimeType, "constraint", _constraint,
            "itemType", _itemType, "mediaType", _mediaType);
        var url :String = DeploymentConfig.staticMediaURL + _itemType + "/" + _mediaType +
            MediaMimeTypes.mimeTypeToSuffix(mimeType);
        return url;
    }

    // from MediaDesc
    override public function isBleepable () :Boolean
    {
        return false;
    }

    /**
     * Returns the type of item for which we're providing static media.
     */
    public function getItemType () :String
    {
        return _itemType;
    }

    /**
     * Returns the media type for which we're obtaining the static default.
     */
    public function getMediaType () :String
    {
        return _mediaType;
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _itemType = (ins.readField(String) as String);
        _mediaType = (ins.readField(String) as String);
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(_itemType);
        out.writeField(_mediaType);
    }

    protected var _itemType :String;
    protected var _mediaType :String;
}
}
