//
// $Id: ActorInfo.as 18101 2009-09-16 21:22:48Z ray $

package com.threerings.msoy.data.all {

import com.threerings.util.Byte;
import com.threerings.util.Equalable;

public interface EntityMedia extends Equalable
{
    /**
     * Returns the path of the URL that references this media.
     */
    function getMediaPath () :String;

    /**
     * Returns the mime type of this media.
     */
    function getMimeType () :int;
}
}
