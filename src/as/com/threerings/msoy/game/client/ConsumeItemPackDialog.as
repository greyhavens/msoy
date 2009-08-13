//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.flex.FlexUtil;
import com.threerings.util.StringUtil;


import com.whirled.game.client.ContentService;
import com.whirled.game.data.GameData;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displays a request for the user to confirm the consumption of an item pack.
 */
public class ConsumeItemPackDialog extends FloatingPanel
{
    /**
     * Displays a dialog confirming the consumption of the specified item pack. If the dialog is
     * confirmed, the consumption is initiated via the supplied client and service.
     *
     * @return true if the dialog was shown, false if it was not because another was already
     * showing or the player was known not to own a single copy of the content pack in question.
     */
    public static function show (
        ctx :MsoyContext, contentService :ContentService,
        gameData :Array /*of GameData*/, ident :String, msg :String) :Boolean
    {
        // look up the metadata for the item pack
        var pdata :GameData = null;
        for each (var gd :GameData in gameData) {
            if (gd.getType() == GameData.ITEM_DATA && gd.ident == ident) {
                pdata = gd;
                break;
            }
        }
        if (pdata == null) { // shouldn't be possible, but better safe than sorry
            return false;
        }

        // if we're already displaying a consume dialog, disallow showing another
        if (_consumeDialog != null) {
            return false;
        }

        // display the confirmation dialog
        _consumeDialog = new ConsumeItemPackDialog(ctx, pdata.name, msg, function () :void {
            contentService.consumeItemPack(0, ident, ctx.listener());
        });
        _consumeDialog.addCloseCallback(function () :void {
            _consumeDialog = null;
        });
        _consumeDialog.open(true);
        return true;
    }

    public function ConsumeItemPackDialog (
        ctx :MsoyContext, name :String, msg :String, onConfirm :Function)
    {
        super(ctx, Msgs.GAME.get("t.consume_item_pack"));
        _onConfirm = onConfirm;
        if (!StringUtil.isBlank(msg)) {
            addChild(FlexUtil.createText(msg, 300));
        }
        addChild(FlexUtil.createText(Msgs.GAME.get("m.consume_item_pack", name), 300));
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case CANCEL_BUTTON: return Msgs.GAME.get("b.no");
        case OK_BUTTON: return Msgs.GAME.get("b.yes");
        default: return super.getButtonLabel(buttonId)
        }
    }

    override protected function okButtonClicked () :void
    {
        _onConfirm();
    }

    protected var _onConfirm :Function;

    protected static var _consumeDialog :ConsumeItemPackDialog;
}
}
