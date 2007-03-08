//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.data.BuildResult;

/**
 * Defines the project builder interface
 */
public interface ProjectBuilder
{
    public BuildResult build () throws ProjectBuilderException;
}
