//
// $Id$

package com.threerings.msoy.client {

import com.threerings.msoy.data.Page;

/**
 * Encapsulates the string constants and serialization of page command events that may be sent to
 * whirled GWT modules and pages therein. See <code>PageCommandEvent.java</code>.
 */
public class PageCommand
{
    public static const NAME :String = "pageCommand";
    public static const EDIT_PROFILE :String = "editProfile";

    /**
     * Sends a command to edit the profile, if one is currently showing.
     */
    public static function editProfile (ctx :MsoyContext) :void
    {
        send(ctx, Page.PEOPLE, EDIT_PROFILE);
    }

    /**
     * Wraps the given command in an array for sending to GWT.
     */
    public static function serialize (page :Page, command :String) :Array
    {
        return [page.path, command];
    }

    /**
     * Sends the given command to GWT.
     */
    public static function send (ctx :MsoyContext, page :Page, command :String) :void
    {
        if (!ctx.getMsoyClient().getEmbedding().hasGWT()) {
            return;
        }
        ctx.getMsoyClient().dispatchEventToGWT(NAME, serialize(page, command));
    }
}
}
