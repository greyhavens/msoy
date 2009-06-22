//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Runtime representation of a facebook template, mainly for use by admin editor.
 */
public class FacebookTemplate
    implements IsSerializable
{
    /** The code used by msoy to reference this template. */
    public String code = "";

    /** The bundle id registered with facebook used to publish an instance of the template. */
    public long bundleId;
}
