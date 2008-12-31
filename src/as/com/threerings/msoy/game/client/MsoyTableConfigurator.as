//
// $Id$

package com.threerings.msoy.game.client {

import mx.controls.TextInput;

import com.threerings.parlor.client.DefaultFlexTableConfigurator;
import com.threerings.parlor.data.RangeParameter;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.data.ToggleParameter;

import com.threerings.msoy.game.data.MsoyTableConfig;

public class MsoyTableConfigurator extends DefaultFlexTableConfigurator
{
    public function MsoyTableConfigurator (
        players :RangeParameter, watchable :ToggleParameter, prvate :ToggleParameter,
        title :TextInput)
    {
        super(players, watchable, prvate);
        _title = title;
    }

    override protected function createTableConfig () :TableConfig
    {
        return new MsoyTableConfig();
    }

    override protected function flushTableConfig () :void
    {
        super.flushTableConfig();

        (_config as MsoyTableConfig).title = _title.text;
    }

    protected var _title :TextInput;
}

}
