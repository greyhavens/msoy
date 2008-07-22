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
    void clearConsole ();

    /**
     * Appends a line of CompilerOutput to the console.
     */
    void appendCompilerOutput (CompilerOutput line);

    /**
     * Appends a line of CompilerOutput to the console, associated with the given PathElement.
     */
    void appendCompilerOutput (CompilerOutput line, PathElement element);

    /**
     * Display the console.
     */
    void displayConsole ();

    /**
     * Destroys the console window.
     */
    void destroy ();
}