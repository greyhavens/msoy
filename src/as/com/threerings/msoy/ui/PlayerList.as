package com.threerings.msoy.ui {

import mx.core.IFactory;

import com.threerings.flex.DSetList;

/**
 * Contains some common settings for Lists used to render PlayerEntries.
 */
public class PlayerList extends DSetList
{
    public function PlayerList (
        renderer :IFactory, sortFn :Function = null, filterFn :Function = null)
    {
        super(renderer, sortFn, filterFn);
        percentWidth = 100;
        percentHeight = 100;
        variableRowHeight = true;
    }
}
}
