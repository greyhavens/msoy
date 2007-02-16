package com.threerings.msoy.game.client {

import mx.controls.ComboBox;
import mx.controls.Label;

import com.threerings.parlor.game.client.FlexGameConfigurator;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.data.FlashGameConfig;

public class FlashGameConfigurator extends FlexGameConfigurator
{
    override protected function createConfigInterface () :void
    {
        var ctx :WorldContext = (_ctx as WorldContext);

        // TODO: This will get and populate game types?
        _game = new ComboBox();
        _game.dataProvider = [ { label:"Reversi", data:15 },
            { label:"ClickFest", url:"reversi.swf" }, // TODO: fixup
            { label:"BigTwo", url:"bigtwo.swf" }, // TODO: fixup
            { label:"Invalid", url:-1 }];

        var label :Label = new Label();
        label.text = Msgs.GAME.get("l.gameName");

        addControl(label, _game);
    }

    override protected function gotGameConfig () :void
    {
        var fconfig :FlashGameConfig = (_config as FlashGameConfig);

        // TODO: straighten this out once we straighten out how we
        // even get the games to display
    }

    override protected function flushGameConfig () :void
    {
        var fconfig :FlashGameConfig = (_config as FlashGameConfig);

        var sel :Object = _game.selectedItem;
        fconfig.configData = sel["url"];
    }

    /** The game being picked. */
    protected var _game :ComboBox;
}
}
