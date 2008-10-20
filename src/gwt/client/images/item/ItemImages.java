//
// $Id$

package client.images.item;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images for item navigation.
 */
public interface ItemImages extends ImageBundle
{
    @Resource("blank.png")
    AbstractImagePrototype blank ();

    @Resource("audio.png")
    AbstractImagePrototype audio ();

    @Resource("avatar.png")
    AbstractImagePrototype avatar ();

    @Resource("backdrop.png")
    AbstractImagePrototype backdrop ();

    @Resource("furniture.png")
    AbstractImagePrototype furniture ();

    @Resource("game.png")
    AbstractImagePrototype game ();

    @Resource("pet.png")
    AbstractImagePrototype pet ();

    @Resource("photo.png")
    AbstractImagePrototype photo ();

    @Resource("toy.png")
    AbstractImagePrototype toy ();

    @Resource("video.png")
    AbstractImagePrototype video ();
}
