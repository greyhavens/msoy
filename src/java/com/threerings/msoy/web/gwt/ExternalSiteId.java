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

    /** This is a special site for FB connect. We need to find a way of sending the this to the
     * client from the database rather than hardwiring the app id to "1". */
    public static final ExternalSiteId FB_CONNECT_DEFAULT = facebookApp(1);

    /**
     * Creates a site identifier for the given facebook-integrated game.
     */
    public static ExternalSiteId facebookGame (int gameId)
    {
        return new ExternalSiteId(Auther.FACEBOOK, -gameId);
    }

    /**
     * Creates a site identifier for the given facebook application.
     */
    public static ExternalSiteId facebookApp (int appId)
    {
        return new ExternalSiteId(Auther.FACEBOOK, appId);
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

    /**
     * Returns the id of the application that this site refers to, or null if this is not a
     * Facebook site or is for an integrated game.
     */
    public Integer getFacebookAppId ()
    {
        return auther == Auther.FACEBOOK && siteId > 0 ? siteId : null;
    }

    /**
     * Returns the id of the game that this site refers to, or null if this is not a Facebook site
     * or is for an application.
     */
    public Integer getFacebookGameId ()
    {
        return auther == Auther.FACEBOOK && siteId < 0 ? -siteId : null;
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
