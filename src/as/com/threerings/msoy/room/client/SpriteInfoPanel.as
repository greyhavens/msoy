//
// $Id$

package com.threerings.msoy.room.client {

import mx.collections.ArrayCollection;
import mx.controls.List;
import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import com.threerings.util.Util;

import com.threerings.io.TypedArray;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldControlBar;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.ui.FloatingPanel

/**
 * Shows info on a bunch of sprites.
 */
public class SpriteInfoPanel extends FloatingPanel
{
    /**
     * A predicate indicating if the specified ident is kosher to pass to getItemNames()?
     */
    public static function isRealIdent (ident :ItemIdent) :Boolean
    {
        return (ident != null) && (ident.type > 0) && (ident.itemId != 0);
    }

    /**
     * Construct a SpriteInfoPanel.
     */
    public function SpriteInfoPanel (ctx :WorldContext, sprites :Array /* of EntitySprite */)
    {
        super(ctx, Msgs.WORLD.get("t.item_info"));
        showCloseButton = true;

        var idents :TypedArray = TypedArray.create(ItemIdent);
        // wrap each sprite inside an array so that we can fill in the names later
        var data :Array = sprites.map(function (sprite :EntitySprite, ... ignored) :Array {
            // sneak-build the idents array
            var ident :ItemIdent = sprite.getItemIdent();
            if (isRealIdent(ident)) {
                idents.push(ident);
            }
            return [ sprite ];
        });
        _data.source = data;

        _list = new List();
        _list.horizontalScrollPolicy = ScrollPolicy.OFF;
        _list.verticalScrollPolicy = ScrollPolicy.ON;
        _list.selectable = false;
        _list.itemRenderer = new ClassFactory(SpriteInfoRenderer);
        _list.dataProvider = _data;
        addChild(_list);

        // refresh the list when bleeps change
        Prefs.events.addEventListener(Prefs.BLEEPED_MEDIA, bleepChanged);

        var svc :ItemService = ctx.getClient().requireService(ItemService) as ItemService;
        svc.getItemNames(idents, ctx.resultListener(gotItemNames));
    }

    override public function close () :void
    {
        super.close();
        Prefs.events.removeEventListener(Prefs.BLEEPED_MEDIA, bleepChanged);
    }

    override protected function didOpen () :void
    {
        super.didOpen();

        // make sure we're not highlighting all items, it screws with our hover highlight
        var btn :CommandButton = WorldControlBar(_ctx.getControlBar()).hotZoneBtn;
        if (btn.selected) {
            btn.activate();
        }
    }

    /**
     * A result handler for the service request we make.
     */
    protected function gotItemNames (names :Array /* of String */) :void
    {
        // trek through the array, pushing on the name for any idents that we passed to the service
        for each (var data :Array in _data.source) {
            if (isRealIdent(EntitySprite(data[0]).getItemIdent())) {
                data.push(names.shift());
            }
        }
        _data.refresh();
    }

    /**
     * Handle bleep changes. Just globally refresh the list for simplicity.
     * Note: We can't just Util.adapt() _data.refresh as the event listener because then we:
     *  - can't remove it
     *  - or, can't add the listener weakly, because it will get collected while we're up.
     * So we might as well have a real method, this one, to handle it.
     */
    protected function bleepChanged (... ignored) :void
    {
        _data.refresh();
    }

    protected var _list :List;

    protected var _data :ArrayCollection = new ArrayCollection();
}
}

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.EntitySprite;
import com.threerings.msoy.room.client.SpriteInfoPanel;

class SpriteInfoRenderer extends HBox
{
    public function SpriteInfoRenderer ()
    {
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        _type = new Label();
        _type.width = 60;

        _name = new Label();
        _name.width = 160;

        _info = new CommandButton(Msgs.GENERAL.get("b.view_info"));
        _bleep = new CommandButton();

        addEventListener(MouseEvent.ROLL_OVER, handleRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleRoll);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        var arr :Array = value as Array;

        var sprite :EntitySprite = arr[0];
        _type.text = Msgs.GENERAL.get(sprite.getDesc());
        _bleep.setCallback(sprite.viz.toggleBleeped);
        _bleep.enabled = sprite.viz.isBleepable();
        _bleep.label = Msgs.GENERAL.get(sprite.viz.isBleeped() ? "b.unbleep" : "b.bleep");

        var ident :ItemIdent = sprite.getItemIdent();
        _info.setCommand(MsoyController.VIEW_ITEM, ident);
        _info.enabled = SpriteInfoPanel.isRealIdent(ident);

        var name :String = arr[1];
        _name.text = name;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 3);
        setStyle("paddingRight", 3);
        setStyle("verticalAlign", "middle");

        addChild(_type);
        addChild(_name);
        addChild(_info);
        addChild(_bleep);
    }

    protected function handleRoll (event :MouseEvent) :void
    {
        EntitySprite(data[0]).setHovered(event.type == MouseEvent.ROLL_OVER);
    }

    protected var _type :Label;
    protected var _name :Label;
    protected var _info :CommandButton;
    protected var _bleep :CommandButton;
}
