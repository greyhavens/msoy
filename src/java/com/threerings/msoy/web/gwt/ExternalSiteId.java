//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.samskivert.util.ByteEnum;

/**
 * Identifies an external partner site from which we allow authentication and other integration
 * features.
 */
public class ExternalSiteId
    implements IsSerializable
{
    /**
     * Possible sources of authentication.
     */
    public enum Auther
        implements ByteEnum
    {
        FACEBOOK(1),
        OPEN_SOCIAL(2),
        OPEN_ID(3);

        // from ByteEnum
        public byte toByte () {
            return _code;
        }

        Auther (int code) {
            if (code < Byte.MIN_VALUE || code > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("Code out of byte range: " + code);
            }
            _code = (byte)code;
        }

        protected transient byte _code;
    }

    /** This is the only external site that is fully supported at the moment. */
    public static final ExternalSiteId FB_GAMES = new ExternalSiteId(Auther.FACEBOOK, 0);

    /**
     * Gets the identified for the given facebook-integrated game.
     * TODO: fully support facebook-integration for AVRGs, including MOGs
     */
    public static ExternalSiteId facebookGame (int gameId)
    {
        return new ExternalSiteId(Auther.FACEBOOK, gameId);
    }

    /** The authentication source for the site. */
    public Auther auther;

    /** The id of the site. There may be more than one site using the same authentication source,
     * this distinguishes them. The interpretation of the id is left to the specific external site
     * supporting infrastructure. */
    public int siteId;

    /**
     * Creates a new external site identifier with the given values.
     */
    public ExternalSiteId (Auther auther, int siteId)
    {
        this.auther = auther;
        this.siteId = siteId;
    }

    /**
     * Creates a new external site identifier for serialization.
     */
    public ExternalSiteId ()
    {
    }

    @Override // from Object
    public int hashCode ()
    {
        return auther.hashCode() + siteId;
    }

    @Override // from Object
    public boolean equals (Object rhs)
    {
        if (!(rhs instanceof ExternalSiteId)) {
            return false;
        }
        ExternalSiteId other = (ExternalSiteId)rhs;
        return other.auther == auther && other.siteId == siteId;
    }

    @Override // from Object
    public String toString ()
    {
        return auther + ":" + siteId;
    }
}
