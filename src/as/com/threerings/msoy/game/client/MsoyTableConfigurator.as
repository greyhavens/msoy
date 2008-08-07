//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.parlor.client.DefaultFlexTableConfigurator;
import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.data.RangeParameter;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.data.ToggleParameter;

import com.threerings.msoy.game.data.MsoyTableConfig;

public class MsoyTableConfigurator extends DefaultFlexTableConfigurator
{
    public function MsoyTableConfigurator (
        players :RangeParameter, watchable :ToggleParameter = null, prvate :ToggleParameter = null)
    {
        super(players, watchable, prvate);
    }

    override protected function createTableConfig () :TableConfig
    {
        var tconf :MsoyTableConfig = new MsoyTableConfig();
        tconf.title = "It's my table and I can cry if I want to";

        return tconf;
    }
}
 
}
