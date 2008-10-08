//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Photo;

/**
 * Includes the details of a Gallery, and the Photos it contains.
 */
public class GalleryData implements IsSerializable
{
    public Gallery gallery;
    public List<Photo> photos;
    public MemberName owner;

    /** True if some part of the gallery has changed and not yet saved */
    public boolean hasUnsavedChanges;

    public List<Integer> getPhotoIds ()
    {
        List<Integer> ids = new ArrayList<Integer>();
        for (Photo photo : photos) {
            ids.add(photo.itemId);
        }
        return ids;
    }

    @Override public String toString ()
    {
        return "[gallery=" + gallery + ", photos=" + photos + ", ownerId=" + owner.getMemberId()
            + "]";
    }
}
