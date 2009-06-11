//
// $Id$

package com.threerings.msoy.ui {

import com.threerings.flex.LoadedAsset;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * Prepends the serverURL to any String source.
 */
public class MsoyLoadedAsset extends LoadedAsset
{
    public function MsoyLoadedAsset (source :Object)
    {
        super((source is String) ? DeploymentConfig.serverURL + source : source);
    }
}
}
