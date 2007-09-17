//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Provide build results in a list for a project.
 */
public interface Console
{
    /**
     * Clear the console
     */
    public void clearConsole ();

    /**
     * Appends a line of CompilerOutput to the console.
     */
    public void appendCompilerOutput (CompilerOutput line);

    /**
     * Appends a line of CompilerOutput to the console, associated with the given PathElement.
     */
    public void appendCompilerOutput (CompilerOutput line, PathElement element);

    /**
     * Display the console.
     */
    public void displayConsole ();

    /**
     * Destroys the console window.
     */
    public void destroy ();
}