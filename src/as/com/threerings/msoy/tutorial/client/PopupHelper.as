//
// $Id$

package com.threerings.msoy.tutorial.client {

/**
 * Simple interface for performing actions when a tutorial item is shown or dismissed. Helpers
 * are attached to items by <code>TutorialItemBuilder</code>.
 */
public interface PopupHelper
{
    /**
     * Called when a tutorial item is shown.
     */
    function popup () :void;

    /**
     * Called when a tutorial item is dismissed.
     */
    function popdown () :void;
}
}
