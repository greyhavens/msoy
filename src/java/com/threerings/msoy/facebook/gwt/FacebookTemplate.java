//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Runtime representation of a facebook template, mainly for use by admin editor.
 */
public class FacebookTemplate
    implements IsSerializable, Comparable<FacebookTemplate>
{
    /** Used by msoy to reference the functionality of this template. */
    public String code = "";

    /** Distinguish between functionally equivalent templates. */
    public String variant = "";

    /** The bundle id registered with facebook used to publish an instance of the template. */
    public long bundleId;

    /**
     * Creates a template for deserialization.
     */
    public FacebookTemplate ()
    {
    }

    /**
     * Creates a new template with the given fields.
     */
    public FacebookTemplate (String code, String variant, long bundleId)
    {
        this.code = code;
        this.variant = variant;
        this.bundleId = bundleId;
    }

    /**
     * Converts this template card to an entry vector based on its code and variant.
     */
    public String toEntryVector ()
    {
        return toEntryVector(code, variant);
    }

    /**
     * Converts the given fields to an entry vector.
     */
    public static String toEntryVector (String code, String variant)
    {
        return "fb." + code + variant;
    }

    /**
     * Detects equality with another template.
     */
    public boolean equals (FacebookTemplate other)
    {
        return code.equals(other.code) && variant.equals(other.variant) &&
            bundleId == other.bundleId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return code.hashCode() + variant.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return other instanceof FacebookTemplate && equals((FacebookTemplate)other);
    }

    @Override // from Comparable
    public int compareTo (FacebookTemplate o)
    {
        int cmp = code.compareTo(o.code);
        cmp = (cmp == 0) ? variant.compareTo(o.variant) : cmp;
        return cmp;
    }
}
