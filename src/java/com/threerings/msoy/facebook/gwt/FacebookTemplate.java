//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.facebook.gwt.FacebookService.Gender;

/**
 * Runtime representation of a facebook template. Used by the admin editor and story feeder.
 */
public class FacebookTemplate
    implements IsSerializable, Comparable<FacebookTemplate>
{
    public static class Key
        implements IsSerializable, Comparable<Key>
    {
        /** Used by msoy to reference the functionality of this template. */
        public String code = "";

        /** Distinguish between functionally equivalent templates. */
        public String variant = "";

        public Key (String code, String variant)
        {
            this.code = code;
            this.variant = variant;
        }

        /**
         * Detects equality with another template.
         */
        public boolean equalsKey (Key other)
        {
            return code.equals(other.code) && variant.equals(other.variant);
        }

        @Override // from Object
        public int hashCode ()
        {
            return code.hashCode() + variant.hashCode();
        }

        @Override // from Object
        public boolean equals (Object other)
        {
            return other instanceof Key && equalsKey((Key)other);
        }

        @Override // from Comparable
        public int compareTo (Key o)
        {
            int cmp = code.compareTo(o.code);
            cmp = (cmp == 0) ? variant.compareTo(o.variant) : cmp;
            return cmp;
        }

        protected Key ()
        {
        }
    }

    /** The lookup key for this template. */
    public Key key;

    /** Whether this template is considered when the client requests a random template. */
    public boolean enabled;

    /** The bundle id registered with facebook used to publish an instance of the template. 
     * TODO: remove, this is deprecated. */
    public long bundleId;

    /** The caption for passing to publishStream. Note that when requesting templates for
     * editing, this field is null. */
    public String caption;

    /** The available captions to choose from for passing to publishStream. Note that when
     * requesting a template for publishing, this field is null. */
    public Map<Gender, String> captions;

    /** The description for passing to publishStream */
    public String description;

    /** The prompt for passing to publishStream */
    public String prompt;

    /** The text of the link to pass to publishStream */
    // TODO: allow for more links?
    // TODO: allow linkUrl?
    public String linkText;

    /**
     * Creates a new template with the given fields.
     */
    public FacebookTemplate (String code, String variant, long bundleId)
    {
        this(new Key(code, variant), bundleId);
    }

    /**
     * Creates a new template with the given key and bundle id.
     */
    public FacebookTemplate (Key key, long bundleId)
    {
        this.key = key;
        this.bundleId = bundleId;
    }

    /**
     * Converts this template card to an entry vector based on its code and variant.
     */
    public String toEntryVector ()
    {
        return "fb." + key.code + key.variant;
    }

    /**
     * Converts the given fields to an entry vector.
     */
    public static String toEntryVector (String code, String variant)
    {
        return "fb." + code + variant;
    }

    @Override // from Object
    public int hashCode ()
    {
        return key.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return other instanceof FacebookTemplate && key.equals(((FacebookTemplate)other).key);
    }

    @Override // from Comparable
    public int compareTo (FacebookTemplate o)
    {
        return key.compareTo(o.key);
    }

    /**
     * Creates a template for deserialization.
     */
    protected FacebookTemplate ()
    {
    }
}
