//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import mx.core.UIComponent;
import mx.core.Container;
import mx.containers.Canvas;
import mx.controls.Label;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.io.TypedArray;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.ScalingMediaContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.badge.data.all.InProgressBadge;

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
            if (disp != null) {
                _items[idx] = disp;
                disp.x = EDGE_MARGIN + (CELL_WIDTH + CELL_SPACING) * col;
                disp.y = EDGE_MARGIN + (CELL_HEIGHT + CELL_SPACING) * row;
                _grid.addChild(disp);
            }
        }
    }

    protected function createItem (item :HomePageItem) :UIComponent
    {
        // Not filled in... bail
        if (item.getAction() == HomePageItem.ACTION_NONE) {
            return null;
        }

        // Create the image
        var image :UIComponent;
        if (true) {
            var view :ScalingMediaContainer = new ScalingMediaContainer(IMAGE_WIDTH, IMAGE_HEIGHT);
            view.setMediaDesc(item.getImage());
            image = new MediaWrapper(view, IMAGE_WIDTH, IMAGE_HEIGHT, true);

        } else {
            // test code for before we have lots of images
            image = new UIComponent();
            function rand (bits :int) :int { return int(128 + Math.random() * 128) << bits; }
            image.graphics.beginFill(rand(16) + rand(8) + rand(0));
            if (Math.random() < .25) {
                image.graphics.drawRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            } else {
                image.graphics.drawEllipse(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            }
            image.graphics.endFill();
        }

        // create the label
        var label :Label = new Label();
        label.width = IMAGE_WIDTH;
        label.height = LABEL_HEIGHT;
        label.text = resolveItemName(item);
        label.truncateToFit = true;
        label.setStyle("textAlign", "center");
        label.y = IMAGE_HEIGHT;

        // create the cell box
        var cell :Canvas = new Canvas();
        cell.addChild(image);
        cell.addChild(label);
        cell.width = CELL_WIDTH;
        cell.height = CELL_HEIGHT;

        return cell;
    }

    protected function resolveItemName (item :HomePageItem) :String
    {
        if (item.getAction() == HomePageItem.ACTION_BADGE) {
            var badge :InProgressBadge = InProgressBadge(item.getActionData());
            var level :String = badge.levelName;
            return _ctx.xlate(MsoyCodes.PASSPORT_MSGS, badge.nameProp, level);

        } else if (item.getName() == null || item.getName() == "null") {
            return "...";

        } else {
            return item.getName();
        }
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
    protected static const IMAGE_WIDTH :int = 80;
    protected static const IMAGE_HEIGHT :int = 60;
    protected static const LABEL_HEIGHT :int = 20;
    protected static const CELL_WIDTH :int = IMAGE_WIDTH;
    protected static const CELL_HEIGHT :int = IMAGE_HEIGHT + LABEL_HEIGHT;
    protected static const CELL_SPACING :int = 10;
    protected static const EDGE_MARGIN :int = 10;
    protected static const WIDTH :int =
        EDGE_MARGIN * 2 + IMAGE_WIDTH * COLUMNS + CELL_SPACING * (COLUMNS - 1);
    protected static const HEIGHT :int =
        EDGE_MARGIN * 2 + (IMAGE_HEIGHT + LABEL_HEIGHT) * ROWS + CELL_SPACING * (ROWS - 1);
}
}
