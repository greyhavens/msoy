//
// $Id$

package com.threerings.msoy.swiftly.server.build;


/**
 * Defines the project builder interface
 */
public interface ProjectBuilder
{
    public BuildResult build () throws ProjectBuilderException;
}
