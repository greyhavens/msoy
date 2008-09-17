//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.Photo;

/**
 * Includes the details of a Gallery, and the Photos it contains.
 */
public class GalleryData implements IsSerializable
{
    public Gallery gallery;
    public List<Photo> photos;
}
