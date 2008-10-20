//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
public class MsoyTokenRing extends TokenRing
{
    /** Indicates that the user has support privileges. */
    public static final int SUPPORT = (1 << 1);

    /** Indicates that the user has maintainer privileges. */
    public static final int MAINTAINER = (1 << 2);

    /**
     * A default constructor, used when unserializing token rings.
     */
    public MsoyTokenRing ()
    {
    }

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
}
