//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.filters.ColorMatrixFilter;

import mx.core.UIComponent;
import mx.core.Container;
import mx.containers.Tile;
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
    public static var log :Log = Log.getLog(HomePageDialog);

    public function HomePageDialog (ctx :WorldContext)
    {
        super(ctx);

        title = Msgs.GENERAL.get("t.home_page");
        showCloseButton = true;
        styleName = "sexyWindow";
        setStyle("paddingTop", EDGE_MARGIN);
        setStyle("paddingBottom", EDGE_MARGIN);
        setStyle("paddingLeft", EDGE_MARGIN);
        setStyle("paddingRight", EDGE_MARGIN);

        _grid = new Tile();
        _grid.tileWidth = IMAGE_WIDTH;
        _grid.tileHeight = IMAGE_HEIGHT + LABEL_HEIGHT;
        _grid.setStyle("horizontalGap", CELL_SPACING);
        _grid.setStyle("verticalGap", CELL_SPACING);
        addChild(_grid);
        gotItems(TypedArray.create(HomePageItem));

        open();
    }

    public function refresh () :void
    {
        log.info("Requesting home page items");
        var svc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        svc.getHomePageGridItems(_ctx.getClient(), new ResultAdapter(failedToGetItems, gotItems));
    }

    protected function failedToGetItems (cause :String) :void
    {
        log.warning("Failed to get home page items", "cause", cause);
    }

    protected function gotItems (items :TypedArray) :void
    {
        _grid.removeAllChildren();
        var numCells :int = ROWS * COLUMNS;
        for (var ii :int = 0; ii < numCells; ii++) {
            var disp :UIComponent = null;
            if (ii < items.length) {
                disp = createItem(HomePageItem(items[ii]));
            }
            if (disp == null) {
                disp = new UIComponent();
            }
            _grid.addChild(disp);
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

        cell.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            log.info("Got mouse click", "name", label.text);
        });

        cell.addEventListener(MouseEvent.ROLL_OVER, function (evt :MouseEvent) :void {
            label.setStyle("textDecoration", "underline");
            image.filters = [BRIGHTEN_FILTER];
        });

        cell.addEventListener(MouseEvent.ROLL_OUT, function (evt :MouseEvent) :void {
            label.setStyle("textDecoration", "none");
            image.filters = [];
        });

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

    protected var _grid :Tile;

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
    protected static const BRIGHTEN_DELTA :Number = 40;
    protected static const BRIGHTEN_FILTER :ColorMatrixFilter = new ColorMatrixFilter([
        1, 0, 0, 0, BRIGHTEN_DELTA,
        0, 1, 0, 0, BRIGHTEN_DELTA,
        0, 0, 1, 0, BRIGHTEN_DELTA,
        0, 0, 0, 1, 0]);
}
}
