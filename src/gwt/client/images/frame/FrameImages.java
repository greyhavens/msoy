//
// $Id$

package client.images.frame;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Images used by the frame.
 */
public interface FrameImages extends ImageBundle
{
    @Resource("noclient.gif")
    AbstractImagePrototype noclient ();

    @Resource("noclient_hover.gif")
    AbstractImagePrototype noclient_hover ();
}
