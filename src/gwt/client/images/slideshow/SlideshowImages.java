//
// $Id$

package client.images.slideshow;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Contains images for Gallery slideshow navigation, or anywhere else we need play/pause/close
 * buttons. These will be served to the client as one big image for added efficiency.
 */
public interface SlideshowImages extends ClientBundle
{
    @Source("close_default.png")
    ImageResource close_default ();

    @Source("close_down.png")
    ImageResource close_down ();

    @Source("close_over.png")
    ImageResource close_over ();

    @Source("pause_default.png")
    ImageResource pause_default ();

    @Source("pause_down.png")
    ImageResource pause_down ();

    @Source("pause_over.png")
    ImageResource pause_over ();

    @Source("play_default.png")
    ImageResource play_default ();

    @Source("play_down.png")
    ImageResource play_down ();

    @Source("play_over.png")
    ImageResource play_over ();
}
