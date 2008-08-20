/**
 * 
 */
package com.threerings.msoy.group.gwt;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Interface that can be implemented by classes representing an object that has a canonical image
 * associated with it, such as a group or a room.
 */
public interface CanonicalImageData
{
    /**
     * Return a media descriptor for the canonical image if there is one in the object, or null if
     * there is not. Null here doesn't necessarily mean that no such image exists - just that this
     * record has not been populated with one.
     */
    public MediaDesc getCanonicalImage ();

    /**
     * Set the media descriptor for the canonical image of this object.
     */
    public void setCanonicalImage (MediaDesc mediaDesc);    
}
