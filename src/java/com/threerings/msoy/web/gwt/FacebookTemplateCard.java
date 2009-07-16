//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Defines a Facebook template for use on the non-admin client when popping up the feed publish
 * dialog.
 */
public class FacebookTemplateCard
    implements IsSerializable
{
    /** The variant assigned to this template. */
    public String variant;

    /** The bundle id to pass along to Facebook. */
    public long bundleId;

    /**
     * Converts this template card to an entry vector based on the given template code.
     */
    public String toEntryVector (String code)
    {
        return toEntryVector(code, variant);
    }

    /**
     * Converts this given fields to an entry vector.
     */
    public static String toEntryVector (String code, String variant)
    {
        return "fb." + code + variant;
    }
}
