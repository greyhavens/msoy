//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
@com.threerings.util.ActionScript(omit=true)
public class MsoyTokenRing extends TokenRing
{
    /** Indicates that the user has support privileges. */
    public static final int SUPPORT = (1 << 1);

    /** Indicates that the user has maintainer privileges. */
    public static final int MAINTAINER = (1 << 2);

    /** Indicates that the user has greeter privileges. */
    public static final int GREETER = (1 << 3);

    /** Indicates that the user has subscriber privileges. */
    public static final int SUBSCRIBER = (1 << 4);

    /**
     * Constructs a token ring with the supplied set of tokens.
     */
    public MsoyTokenRing (int tokens)
    {
        super(tokens);
    }

    /**
     * Convenience function for checking whether this ring confers support privileges.
     */
    public boolean isSupport ()
    {
        return holdsAnyToken(SUPPORT | ADMIN | MAINTAINER);
    }

    /**
     * Convenience function for checking whether this ring confers maintainer privileges.
     */
    public boolean isMaintainer ()
    {
        return holdsToken(MAINTAINER);
    }

    @Override // from TokenRing
    public boolean isAdmin ()
    {
        return holdsAnyToken(ADMIN | MAINTAINER);
    }

    /**
     * Convenience function for checking whether this ring confers greeter privileges.
     */
    public boolean isGreeter ()
    {
        return holdsToken(GREETER);
    }

    /**
     * Convenience function for checking if the user is a subscriber, allowing support+, too.
     */
    public boolean isSubscriberPlus ()
    {
        return holdsAnyToken(SUPPORT | ADMIN | MAINTAINER | SUBSCRIBER);
    }

    /**
     * Is this user a subscriber, not counting support+.
     */
    public boolean isSubscriber ()
    {
        return holdsToken(SUBSCRIBER);
    }
}
