//
// $Id$

package com.threerings.msoy.item.client {

import mx.containers.Grid;
import mx.controls.TextArea;

import com.threerings.flex.CommandComboBox;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag_Kind;
import com.threerings.msoy.item.data.all.ItemIdent;

public class FlagItemDialog extends FloatingPanel
{
    public function FlagItemDialog (ctx :MsoyContext, ident :ItemIdent)
    {
        super(ctx, Msgs.ITEM.get("t.flag"));
        _ident = ident;

        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(FlexUtil.createWideText(
            Msgs.ITEM.get("m.flag", Msgs.ITEM.get(Item.getTypeKey(_ident.type)))));

        _kind = new CommandComboBox(checkKindSelected);
        _kind.prompt = Msgs.ITEM.get("p.flag_kind");
        _kind.dataProvider = ItemFlag_Kind.values();

        _comment = new TextArea();
        _comment.percentWidth = 100;
        _comment.maxChars = 2047;

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.ITEM.get("l.flag_kind"), _kind);
        GridUtil.addRow(grid, Msgs.ITEM.get("l.flag_comment"), _comment);
        addChild(grid);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
        checkKindSelected();
    }

    protected function checkKindSelected (... ignored) :void
    {
        getButton(OK_BUTTON).enabled = (-1 != _kind.selectedIndex);
    }

    override protected function okButtonClicked () :void
    {
        var isvc :ItemService = _ctx.getClient().requireService(ItemService) as ItemService;
        isvc.addFlag(_ctx.getClient(), _ident, ItemFlag_Kind(_kind.selectedItem), _comment.text,
            _ctx.confirmListener("m.flag_reported", MsoyCodes.ITEM_MSGS));
    }

    protected var _ident :ItemIdent;

    protected var _kind :CommandComboBox;

    protected var _comment :TextArea;
}
}
