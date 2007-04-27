//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

import com.threerings.toybox.data.TableMatchConfig;

/**
 * Class to encapsulate extended properties to table matches for Whirled. 
 */
@ActionScript(omit=true)
public class MsoyMatchConfig extends TableMatchConfig 
{
    /** Whether this game is watchable or not. defaults to watchable */
    public boolean unwatchable = false;
}
