//
// $Id$

package com.threerings.msoy.chat.client {

import flexlib.containers.SuperTabNavigator;
import flexlib.controls.tabBarClasses.SuperTab;

/**
 * Undoes some stupid stuff that SuperTabNavigator does in an attempt to preserve and work around
 * some stupid stuff that TabNavigator does. The stupidity, it breeds. Another useful note: this is
 * a public class because it has to be in order for styles to work. If we made it an inner class
 * of, say, ChatChannelPanel, then SuperTabNavigator and TabNavigator styles magically stop
 * working. Yay!
 */
public class ChatTabNavigator extends SuperTabNavigator
{
    public function ChatTabNavigator ()
    {
        popUpButtonPolicy = POPUPPOLICY_OFF;
        dragEnabled = false;

        // this adjusts the gap between the *bottom* of the tabs and the contents; you might thing
        // it referred to the padding at the top of the TabNavigator panel, but that would be far
        // too straightforward for those clever Flex engineers; on top of that, Flex hardcodes a
        // one pixel overlap for the tab bars because the default style works that way, so we're
        // actually setting a 4 pixel gap between the bottom of the tabs and the top of the
        // contents of the tab here; amazing!
        setStyle("paddingTop", 5);
    }

    override protected function createChildren() :void
    {
        super.createChildren();

        // fortunately we can set our tab bar background here because it's otherwise impossible
        // with the normal TabNavigator
        holder.setStyle("backgroundColor", 0x222222);
    }

    override protected function updateDisplayList (uWidth :Number, uHeight :Number) :void
    {
        super.updateDisplayList(uWidth, uHeight);

        // the Flex engineers, in their infinite wisdom, hardcoded a 1 pixel overlay between their
        // tabs and the contents of their TabNavigator; so SuperTabNavigator, in an attempt to
        // preserve this behavior, moves their tab holder down a pixel, introducing a one pixel
        // tall blank row across the top (which TabNavigator doesn't have, whoops); so we undo that
        // because we never wanted the one pixel overlap in the first place
        holder.move(0, 0);

        // SuperTabNavigator goes on to size its scroll buttons one pixel shorter than the actual
        // tab height because they don't want them extending down into the one pixel overlap
        // region; brilliant! we don't want the overlap and we want full-height buttons; the
        // "tabBarHeight + 1" business is because TabNavigator actually returns the height of the
        // tabs -1 when you ask for the tabBarHeight so as to accomplish its bullshit one pixel
        // overlap; oh the twisty maze of passages!
        canvas.explicitButtonHeight = (tabBarHeight + 1);
    }
}
}
