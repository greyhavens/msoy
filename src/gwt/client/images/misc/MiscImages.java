//
// $Id$

package client.images.misc;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Contains images for miscellaneous bits.
 */
public interface MiscImages extends ClientBundle
{
    @Source("share.png")
    ImageResource share ();

    @Source("add_favorite.png")
    ImageResource add_favorite ();

    @Source("favorite.png")
    ImageResource favorite ();
}
