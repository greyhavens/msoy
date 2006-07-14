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
     * Convenience function for checking whether this ring holds the {@link
     * #SUPPORT} token.
     */
    public boolean isSupport ()
    {
        return holdsToken(SUPPORT);
    }
}
