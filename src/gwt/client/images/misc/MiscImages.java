//
// $Id$

package client.images.misc;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images for miscellaneous bits.
 */
public interface MiscImages extends ImageBundle
{
    @Resource("share.png")
    AbstractImagePrototype share ();

    @Resource("add_favorite.png")
    AbstractImagePrototype add_favorite ();

    @Resource("favorite.png")
    AbstractImagePrototype favorite ();
}
