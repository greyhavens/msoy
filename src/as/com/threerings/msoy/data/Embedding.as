//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Enum;

/**
 * Enumerates the ways in which the flash client can be embedded. Traditionally this is either "on
 * whirled.com" or not. This class was added to also incorporate the notion of being embedded on
 * Facebook. The behavior in this scenario overlaps partially with traditional embeds and
 * partially with whirled.com. As much as possible, individual methods define the specific
 * categories of overlapping behavior.
 *
 * <p>Note that the gwt Embedding enumeration is a nested class in <code>client.frame.Frame</code>.
 * The value does not need to be serialized (so class names don't have to match). Furthermore, they
 * are not exactly analogous because gwt is never embedded independently like the flash client (it
 * does not make sense to include the <code>OTHER</code> value there).</p>
 */
public final class Embedding extends Enum
{
    /** Not embedded, i.e. the user is looking at whirled.com or a dev deployment. */
    public static const NONE :Embedding = new Embedding("NONE");

    /** Embedded on facebook, gwt is available but there are other limits. */
    public static const FACEBOOK :Embedding = new Embedding("FACEBOOK");

    /** Embedded elsewhere, no gwt interface. */
    public static const OTHER :Embedding = new Embedding("OTHER");

    /** Embedded on facebook for rooms app, gwt is available but there are other limits. */
    public static const FACEBOOK_ROOMS :Embedding = new Embedding("FACEBOOK_ROOMS");

    public static const WHIRLED_DJ :Embedding = new Embedding("WHIRLED_DJ");

    finishedEnumerating(Embedding);

    /**
     * Gets the value of the enumeration with the given name.
     */
    public static function valueOf (name :String) :Embedding
    {
        return Enum.valueOf(Embedding, name) as Embedding;
    }

    /**
     * Gets an array of all enumerated values.
     */
    public static function values () :Array
    {
        return Enum.values(Embedding);
    }

    /** @private */
    public function Embedding (name :String)
    {
        super(name);
    }

    /**
     * Returns true if this embedding is expected to have access to the gwt interface.
     */
    public function hasGWT () :Boolean
    {
        return this != OTHER;
    }

    /**
     * Returns true if we have a nice thick header bar for the chat tabs.
     */
    public function hasThickHeader () :Boolean
    {
        return this == NONE || this == FACEBOOK_ROOMS || this == WHIRLED_DJ;
    }

    /**
     * Returns true if we have space for white margins around the edges of rooms and games.
     */
    public function hasPlaceMargins () :Boolean
    {
        return this == NONE;
    }

    /**
     * Returns true if this embedding wants to hide the control bars and other things when gwt is
     * open.
     */
    public function isChromelessWhenMinimized () :Boolean
    {
        return this == FACEBOOK_ROOMS;
    }

    /**
     * Returns true if this embedding should upsell whirled (i.e. display some links to whirled).
     */
    public function shouldUpsellWhirled () :Boolean
    {
        return this == OTHER;
    }

    /**
     * Returns true if this embedding should hide room editting and other features.
     */
    public function isMinimal () :Boolean
    {
        return this == WHIRLED_DJ;
    }
}
}
