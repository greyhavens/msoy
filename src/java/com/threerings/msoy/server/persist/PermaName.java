package com.threerings.msoy.server.persist;

import com.threerings.util.Name;

/**
 * Representation of a permaname.  Useful for various operations such as normalization.
 */
public class PermaName extends Name 
{    
    /** Construct a new permaname. */
    public PermaName (String name) {
        super(name);
    }
    
    // This is a placeholder so that PermaNames are a distinct type.  Currently the functionality
    // is the same as com.threering.util.Name;
}