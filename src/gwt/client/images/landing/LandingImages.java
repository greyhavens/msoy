//
// $Id$

package client.images.landing;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images used by the WhatIsWhirled panel.
 */
public interface LandingImages extends ImageBundle
{
    /**
     * @gwt.resource login.png
     */
    AbstractImagePrototype login ();

    /**
     * @gwt.resource login_over.png
     */
    AbstractImagePrototype login_over ();

    /**
     * @gwt.resource signup.png
     */
    AbstractImagePrototype signup ();

    /**
     * @gwt.resource signup_over.png
     */
    AbstractImagePrototype signup_over ();

    /**
     * @gwt.resource playgames.png
     */
    AbstractImagePrototype playgames ();

    /**
     * @gwt.resource playgames_over.png
     */
    AbstractImagePrototype playgames_over ();
}
