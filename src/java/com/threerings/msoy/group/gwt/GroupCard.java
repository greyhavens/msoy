//
// $Id$

package com.threerings.msoy.group.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Group;

/**
 * Contains a group's name, logo and home scene.
 */
public class GroupCard
    implements IsSerializable, CanonicalImageData
{
    /** The group's name. */
    public GroupName name;

    /** The groups's logo (or the default). */
    public MediaDesc logo = Group.getDefaultGroupLogoMedia();
    
    /** This group's brief description. */
    public String blurb;

    /** The scene id of this group's hall. */
    public int homeSceneId;

    /** The canonical image from the group's home scene */
    public MediaDesc canonicalImage;
    
    /** The number of people online in this group's scenes (as of the last snapshot). */
    public int population;

    /** The canonical image from the group's home scene. */
    public MediaDesc getCanonicalImage () {
        return canonicalImage;
    }

    /** Set the canonical image for this group **/
    public void setCanonicalImage (MediaDesc mediaDesc) {
        canonicalImage = mediaDesc;
    }
}
