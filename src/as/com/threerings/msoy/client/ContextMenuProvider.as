//
// $Id$

package com.threerings.msoy.client {

/**
 * An interface that should be implemented by DisplayObjects that wish
 * to add custom menu items to the context menu.
 */
public interface ContextMenuProvider
{
    /**
     * Called to add to the array of custom menu items.
     */
    function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void;
}
}
