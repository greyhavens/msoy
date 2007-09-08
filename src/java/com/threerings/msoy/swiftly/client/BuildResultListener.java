//
// $Id$
package com.threerings.msoy.swiftly.client;

import com.threerings.msoy.swiftly.data.BuildResult;

public interface BuildResultListener
{
    public void gotResult (BuildResult result);
}
