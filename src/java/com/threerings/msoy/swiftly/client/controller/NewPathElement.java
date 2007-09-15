//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Holds the fields necessary for creating a new PathElement.
 */
public class NewPathElement
{
    public final String name;
    public final PathElement parent;
    public final String mimeType;

    public NewPathElement (String name, PathElement parent, String mimeType)
    {
        this.name = name;
        this.parent = parent;
        this.mimeType = mimeType;
    }
}
