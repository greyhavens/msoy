//
// $Id$
package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.CompilerOutput;

/**
 * A view capable of displaying a BuildResult.
 */
public interface CompilerOutputComponent
{
    /**
     * Display the given CompilerOutput line in the gutter.
     */
    public void displayCompilerOutput (CompilerOutput line);

    /**
     * Clears any CompilerOutput currently shown on this gutter.
     */
    public void clearCompilerOutput ();
}
