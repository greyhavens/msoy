//
// $Id$

package client.images.navi;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Images used in the header.
 */
public interface NaviImages extends ImageBundle
{
    @Resource("shop.png")
    AbstractImagePrototype shop ();

    @Resource("shop_over.png")
    AbstractImagePrototype oshop ();

    @Resource("shop_selected.png")
    AbstractImagePrototype sshop ();

    @Resource("games.png")
    AbstractImagePrototype games ();

    @Resource("games_over.png")
    AbstractImagePrototype ogames ();

    @Resource("games_selected.png")
    AbstractImagePrototype sgames ();

    @Resource("help.png")
    AbstractImagePrototype help ();

    @Resource("help_over.png")
    AbstractImagePrototype ohelp ();

    @Resource("help_selected.png")
    AbstractImagePrototype shelp ();

    @Resource("me.png")
    AbstractImagePrototype me ();

    @Resource("me_over.png")
    AbstractImagePrototype ome ();

    @Resource("me_selected.png")
    AbstractImagePrototype sme ();

    @Resource("friends.png")
    AbstractImagePrototype friends ();

    @Resource("friends_over.png")
    AbstractImagePrototype ofriends ();

    @Resource("friends_selected.png")
    AbstractImagePrototype sfriends ();

    @Resource("worlds.png")
    AbstractImagePrototype worlds ();

    @Resource("worlds_over.png")
    AbstractImagePrototype oworlds ();

    @Resource("worlds_selected.png")
    AbstractImagePrototype sworlds ();
}
