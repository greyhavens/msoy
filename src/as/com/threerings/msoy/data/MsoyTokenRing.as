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
}
}
