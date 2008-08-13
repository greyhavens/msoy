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
    @Resource("login.png")
    AbstractImagePrototype login ();

    @Resource("login_over.png")
    AbstractImagePrototype login_over ();

    @Resource("signup.png")
    AbstractImagePrototype signup ();

    @Resource("signup_over.png")
    AbstractImagePrototype signup_over ();

    @Resource("playgames.png")
    AbstractImagePrototype playgames ();

    @Resource("playgames_over.png")
    AbstractImagePrototype playgames_over ();
}
