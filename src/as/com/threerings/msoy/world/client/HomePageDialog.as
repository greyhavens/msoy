//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import mx.core.UIComponent;
import mx.core.Container;

import com.threerings.util.Log;
import com.threerings.io.TypedArray;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.HomePageItem;

public class HomePageDialog extends FloatingPanel
{
    public function HomePageDialog (ctx :WorldContext)
    {
        super(ctx);

        title = Msgs.GENERAL.get("t.home_page");
        showCloseButton = true;
        styleName = "sexyWindow";
        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 0);
        setStyle("paddingRight", 0);

        _grid = new Container();
        _grid.graphics.beginFill(0x000000);
        _grid.graphics.drawRect(0, 0, WIDTH, HEIGHT);
        _grid.graphics.endFill();
        _grid.width = WIDTH;
        _grid.height = HEIGHT;

        addChild(_grid);

        open();
    }

    public function refresh () :void
    {
        Log.getLog(this).info("Requesting home page items");
        var svc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        svc.getHomePageGridItems(_ctx.getClient(), new ResultAdapter(failedToGetItems, gotItems));
    }

    protected function failedToGetItems (cause :String) :void
    {
        Log.getLog(this).warning("Failed to get home page items", "cause", cause);
    }

    protected function gotItems (items :TypedArray) :void
    {
        var ii :int = 0;
        for each (var item :HomePageItem in items) {
            setItemAt(ii / COLUMNS, ii % COLUMNS, item);
            ii++;
        }
    }

    protected function setItemAt (row :int, col :int, item :HomePageItem) :void
    {
        if (row >= ROWS || col >= COLUMNS) {
            return;
        }

        var idx :int = row * COLUMNS + col;
        if (_items[idx] != null) {
            _grid.removeChild(_items[idx]);
            _items[idx] = null;
        }

        if (item != null) {
            var disp :UIComponent = createItem(item);
            _items[idx] = disp;
            disp.x = EDGE_MARGIN + (CELL_SIZE + CELL_SPACING) * col;
            disp.y = EDGE_MARGIN + (CELL_SIZE + CELL_SPACING) * row;
            _grid.addChild(disp);
        }
    }

    protected function createItem (item :HomePageItem) :UIComponent
    {
        var sprite :UIComponent = new UIComponent();

        function rand (bits :int) :int { return int(128 + Math.random() * 128) << bits; }
        sprite.graphics.beginFill(rand(16) + rand(8) + rand(0));
        if (Math.random() < .25) {
            sprite.graphics.drawRect(0, 0, CELL_SIZE, CELL_SIZE);
        } else {
            sprite.graphics.drawCircle(CELL_SIZE / 2, CELL_SIZE / 2, CELL_SIZE / 2);
        }
        sprite.graphics.endFill();
        return sprite;
    }

    override protected function didOpen () :void
    {
        super.didOpen();
        refresh();
    }

    protected var _grid :UIComponent;
    protected var _items :Array = [];

    protected static const ROWS :int = 3;
    protected static const COLUMNS :int = 3;
    protected static const CELL_SIZE :int = 100;
    protected static const CELL_SPACING :int = 10;
    protected static const EDGE_MARGIN :int = 10;
    protected static const WIDTH :int =
        EDGE_MARGIN * 2 + CELL_SIZE * COLUMNS + CELL_SPACING * (COLUMNS - 1);
    protected static const HEIGHT :int =
        EDGE_MARGIN * 2 + CELL_SIZE * ROWS + CELL_SPACING * (ROWS - 1);
}
}
