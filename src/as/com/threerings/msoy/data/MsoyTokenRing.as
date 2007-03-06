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

    /**
     * Constructs a token ring with the supplied set of tokens.
     */
    public function MsoyTokenRing (tokens :int = 0)
    {
        super(tokens);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link
     * #SUPPORT} token OR a the ADMIN token.
     */
    public function isSupport () :Boolean
    {
        return holdsAnyToken(SUPPORT | ADMIN);
    }
}
}
