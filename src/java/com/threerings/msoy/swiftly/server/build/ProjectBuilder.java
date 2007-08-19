//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.io.File;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.data.BuildResult;

/**
 * Defines the project builder interface
 */
public interface ProjectBuilder
{
    /**
     * Build the given project in the provided build directory. It is the
     * caller's responsibility to clean this directory.
     */
    public BuildResult build (File buildDir, MemberName member) throws ProjectBuilderException;
}
