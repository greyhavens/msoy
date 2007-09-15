//
// $Id$
package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.data.BuildResult;

/**
 * A view capable of displaying a BuildResult.
 */
public interface BuildResultComponent
{
    /**
     * Display the given BuildResult in the view.
     */
    public void displayBuildResult (BuildResult result);
}
