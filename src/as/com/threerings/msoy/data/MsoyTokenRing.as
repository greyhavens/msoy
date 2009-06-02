//
// $Id$

package com.threerings.msoy.data {

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
public class MsoyTokenRing extends TokenRing
{
    /** Indicates that the user has support privileges. */
    public static const SUPPORT :int = (1 << 1);

    /** Indicates that the user has maintainer privileges. */
    public static const MAINTAINER :int = (1 << 2);

    /** Indicates that the user has greeter privileges. */
    public static const GREETER :int = (1 << 3);

    /** Indicates that the user has subscriber privileges. */
    public static const SUBSCRIBER :int = (1 << 4);

    /**
     * Constructs a token ring with the supplied set of tokens.
     */
    public function MsoyTokenRing (tokens :int = 0)
    {
        super(tokens);
    }

    /**
     * Convenience function for checking whether this ring has support privileges.
     */
    public function isSupport () :Boolean
    {
        return holdsAnyToken(SUPPORT | ADMIN | MAINTAINER);
    }

    /**
     * Convenience function for checking whether this ring confers maintainer privileges.
     */
    public function isMaintainer () :Boolean
    {
        return holdsToken(MAINTAINER);
    }

    // from TokenRing
    override public function isAdmin () :Boolean
    {
        return holdsAnyToken(ADMIN | MAINTAINER);
    }

    /**
     * Convenience function for checking if the user is a subscriber, allowing support+, too.
     */
    public function isSubscriberPlus () :Boolean
    {
        return holdsAnyToken(SUPPORT | ADMIN | MAINTAINER | SUBSCRIBER);
    }

    /**
     * Is this user a subscriber, not counting support+.
     */
    public function isSubscriber () :Boolean
    {
        return holdsToken(SUBSCRIBER);
    }
}
}
