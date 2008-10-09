//
// $Id: StuffImages.java 12359 2008-10-08 00:50:41Z sarah $

package client.images.slideshow;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images for Gallery slideshow navigation, or anywhere else we need play/pause/close
 * buttons. These will be served to the client as one big image for added efficiency.
 */
public interface SlideshowImages extends ImageBundle
{
    @Resource("close_default.png")
    AbstractImagePrototype close_default ();

    @Resource("close_down.png")
    AbstractImagePrototype close_down ();

    @Resource("close_over.png")
    AbstractImagePrototype close_over ();

    @Resource("pause_default.png")
    AbstractImagePrototype pause_default ();

    @Resource("pause_down.png")
    AbstractImagePrototype pause_down ();

    @Resource("pause_over.png")
    AbstractImagePrototype pause_over ();

    @Resource("play_default.png")
    AbstractImagePrototype play_default ();

    @Resource("play_down.png")
    AbstractImagePrototype play_down ();

    @Resource("play_over.png")
    AbstractImagePrototype play_over ();
}
