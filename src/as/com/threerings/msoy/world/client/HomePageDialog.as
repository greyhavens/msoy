//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.filters.ColorMatrixFilter;
import flash.geom.Rectangle;

import mx.core.UIComponent;
import mx.core.Container;
import mx.containers.Tile;
import mx.containers.Canvas;
import mx.controls.Text;
import mx.events.CloseEvent;

import com.threerings.flash.GraphicsUtil;
import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.PopUpUtil;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.io.TypedArray;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.ScalingMediaContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.AVRGameNavItemData;
import com.threerings.msoy.data.BasicNavItemData;
import com.threerings.msoy.badge.data.all.BadgeCodes;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.item.data.all.Item;

/**
 * The "My Whirled Places" 3x3 grid of recent games and rooms you have visited, plus stamps and
 * special actions like the Whirled Tour.  Displayed when landing in your home room.
 */
public class HomePageDialog extends FloatingPanel
{
    public static var log :Log = Log.getLog(HomePageDialog);

    public function HomePageDialog (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
        
        // when the close button is pressed, log a tracking event
        addEventListener(CloseEvent.CLOSE, function () :void {
            _wctx.getMsoyClient().trackClientAction("whirledPlacesCloseClicked", null);
        });

        title = Msgs.GENERAL.get("t.home_page");
        showCloseButton = true;
        setStyle("paddingTop", EDGE_MARGIN);
        setStyle("paddingLeft", EDGE_MARGIN);
        setStyle("paddingRight", EDGE_MARGIN);
        setStyle("paddingBottom", 2);
        setStyle("verticalGap", 0);
        setStyle("horizontalAlign", "left");

        // Set up the tile container for the items
        _grid = new Tile();
        _grid.tileWidth = IMAGE_WIDTH;
        _grid.tileHeight = IMAGE_HEIGHT + LABEL_HEIGHT;
        _grid.setStyle("horizontalGap", CELL_HSPACING);
        _grid.setStyle("verticalGap", CELL_VSPACING);
        addChild(_grid);

        var autoshow :CommandCheckBox = new CommandCheckBox(
            Msgs.HOME_PAGE_GRID.get("b.autoshow"), Prefs.setGridAutoshow);
        autoshow.selected = Prefs.getGridAutoshow();
        addChild(autoshow);

        // Fill it with empty components just to force the layout
        gotItems(TypedArray.create(HomePageItem));

        open();
    }

    override public function stylesInitialized () :void
    {
        super.stylesInitialized();

        // Draw some dashed lines between the cells
        var color :int = getStyle("borderColor") as int;
        _grid.graphics.lineStyle(0.5, color, 1.0);
        for (var row :int = 1; row < ROWS; ++row) {
            var y :Number = row * (CELL_HEIGHT + CELL_VSPACING) - CELL_VSPACING / 2;
            var x1 :Number = CELL_WIDTH * COLUMNS + CELL_HSPACING * (COLUMNS - 1);
            GraphicsUtil.dashTo(_grid.graphics, 0, y, x1, y, 3, 3);
        }
        for (var col :int = 1; col < COLUMNS; ++col) {
            var x :Number = col * (CELL_WIDTH + CELL_HSPACING) - CELL_HSPACING / 2;
            var y1 :Number = CELL_HEIGHT * ROWS + CELL_VSPACING * (ROWS - 1);
            GraphicsUtil.dashTo(_grid.graphics, x, 0, x, y1, 3, 3);
        }
    }

    public function refresh () :void
    {
        log.info("Requesting home page items");
        var svc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        svc.getHomePageGridItems(_ctx.getClient(), new ResultAdapter(gotItems, failedToGetItems));
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
        var view :ScalingMediaContainer = new ScalingMediaContainer(IMAGE_WIDTH, IMAGE_HEIGHT);
        view.setMediaDesc(item.getImage());
        var image :UIComponent = new MediaWrapper(view, IMAGE_WIDTH, IMAGE_HEIGHT, true);

        // create the label
        var label :Text = new Text();
        label.width = IMAGE_WIDTH;
        label.height = LABEL_HEIGHT;
        label.text = resolveItemText(item);
        label.truncateToFit = true;
        label.setStyle("textAlign", "center");
        label.setStyle("verticalAlign", "middle");
        label.setStyle("fontFamily", "Arial");
        label.setStyle("fontSize", "11");
        label.y = IMAGE_HEIGHT;

        // create the cell box
        var cell :Canvas = new Canvas();
        cell.addChild(image);
        cell.addChild(label);
        cell.width = CELL_WIDTH;
        cell.height = CELL_HEIGHT;
        cell.useHandCursor = true;
        cell.buttonMode = true;
        cell.mouseChildren = false;

        cell.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            itemClicked(item);
            close();
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

    protected function resolveItemText (item :HomePageItem) :String
    {
        var basicData :BasicNavItemData = item.getNavItemData() as BasicNavItemData;
        var name :String = basicData != null ? basicData.getName() : "?";

        switch (item.getAction()) {
        case HomePageItem.ACTION_BADGE:
            var badge :InProgressBadge = InProgressBadge(item.getNavItemData());
            var level :String = badge.levelName;
            var badgeName :String = Msgs.PASSPORT.get(badge.nameProp, level);
            var badgeDesc :String;
            if (Msgs.PASSPORT.exists(badge.descProp)) {
                badgeDesc = Msgs.PASSPORT.get(badge.descProp);
            } else {
                badgeDesc = Msgs.PASSPORT.get(badge.descPropGeneric, String(badge.levelUnits));
            }
            return Msgs.HOME_PAGE_GRID.get("b.earn_badge", badgeName, badgeDesc);

        case HomePageItem.ACTION_ROOM:
            return Msgs.HOME_PAGE_GRID.get("b.visit_room", name);

        case HomePageItem.ACTION_GROUP:
            return Msgs.HOME_PAGE_GRID.get("b.visit_group", name);

        case HomePageItem.ACTION_GAME:
        case HomePageItem.ACTION_AVR_GAME:
            return Msgs.HOME_PAGE_GRID.get("b.play_game", name);

        case HomePageItem.ACTION_EXPLORE:
            return Msgs.WORLD.get("b.start_tour");

        default:
            return name;
        }
    }

    override protected function didOpen () :void
    {
        // Vertical center in place view, and against right edge with padding
        var placeBounds :Rectangle = _wctx.getPlaceViewBounds();
        y = placeBounds.y + (placeBounds.height - height) / 2;
        x = placeBounds.right - width - PADDING;

        super.didOpen();

        refresh();
    }

    protected function itemClicked (item :HomePageItem) :void
    {
        var trackingDetails :String;
        switch (item.getAction()) {

        case HomePageItem.ACTION_GAME:
            trackingDetails = "game_" + BasicNavItemData(item.getNavItemData()).getId();
            _wctx.getWorldController().handleJoinGameLobby(
                BasicNavItemData(item.getNavItemData()).getId());
            break;

        case HomePageItem.ACTION_AVR_GAME:
            trackingDetails = "avrgame_" + BasicNavItemData(item.getNavItemData()).getId();
            _wctx.getWorldController().handleGoGroupHome(
                AVRGameNavItemData(item.getNavItemData()).getGroupId());
            break;

        case HomePageItem.ACTION_BADGE:
            trackingDetails = "badge_" + InProgressBadge(item.getNavItemData()).badgeCode;
            badgeClicked(InProgressBadge(item.getNavItemData()).badgeCode);
            break;

        case HomePageItem.ACTION_GROUP:
            trackingDetails = "group_" + BasicNavItemData(item.getNavItemData()).getId();
            _wctx.getWorldController().handleGoGroupHome(
                BasicNavItemData(item.getNavItemData()).getId());
            break;

        case HomePageItem.ACTION_ROOM:
            trackingDetails = "room_" + BasicNavItemData(item.getNavItemData()).getId();
            _wctx.getWorldController().handleGoScene(
                BasicNavItemData(item.getNavItemData()).getId());
            break;

        case HomePageItem.ACTION_EXPLORE:
            trackingDetails = "tour"
            startTour();
            break;

        default:
            trackingDetails = "UNKNOWN"
            log.info("No action for " + item);
            break;
        }
        
        _wctx.getMsoyClient().trackClientAction("whirledPlacesItemClicked", trackingDetails);
    }

    protected function badgeClicked (code :int) :void
    {
        var ctrl :WorldController = _wctx.getWorldController();
        switch (uint(code)) {
        case BadgeCodes.FRIENDLY:
        case BadgeCodes.FIXTURE:
            ctrl.displayPage("whirleds", "");
            break;

        case BadgeCodes.EXPLORER:
            startTour();
            break;

        case BadgeCodes.MAGNET:
            ctrl.displayPage("people", "invites");
            break;

        case BadgeCodes.GAMER:
        case BadgeCodes.CONTENDER:
        case BadgeCodes.COLLECTOR:
            ctrl.displayPage("games", "");
            break;

        case BadgeCodes.CHARACTER_DESIGNER:
            ctrl.displayPage("stuff", "" + Item.AVATAR);
            break;

        case BadgeCodes.FURNITURE_BUILDER:
            ctrl.displayPage("stuff", "" + Item.FURNITURE);
            break;

        case BadgeCodes.LANDSCAPE_PAINTER:
            ctrl.displayPage("stuff", "" + Item.DECOR);
            break;

        case BadgeCodes.PROFESSIONAL:
        case BadgeCodes.ARTISAN:
        case BadgeCodes.SHOPPER:
        case BadgeCodes.JUDGE:
        case BadgeCodes.OUTSPOKEN:
            ctrl.displayPage("shop", "");            
            break;
        }
    }

    protected function startTour () :void
    {
        _wctx.getTourDirector().startTour();
    }

    protected var _grid :Tile;
    protected var _wctx :WorldContext;

    protected static const ROWS :int = 3;
    protected static const COLUMNS :int = 3;
    protected static const IMAGE_WIDTH :int = 120;
    protected static const IMAGE_HEIGHT :int = 90;
    protected static const LABEL_HEIGHT :int = 40;
    protected static const CELL_WIDTH :int = IMAGE_WIDTH;
    protected static const CELL_HEIGHT :int = IMAGE_HEIGHT + LABEL_HEIGHT;
    protected static const CELL_HSPACING :int = 30;
    protected static const CELL_VSPACING :int = 15;
    protected static const EDGE_MARGIN :int = 20;
    protected static const WIDTH :int =
        EDGE_MARGIN * 2 + IMAGE_WIDTH * COLUMNS + CELL_HSPACING * (COLUMNS - 1);
    protected static const HEIGHT :int =
        EDGE_MARGIN * 2 + (IMAGE_HEIGHT + LABEL_HEIGHT) * ROWS + CELL_VSPACING * (ROWS - 1);
    protected static const BRIGHTEN_DELTA :Number = 40;
    protected static const BRIGHTEN_FILTER :ColorMatrixFilter = new ColorMatrixFilter([
        1, 0, 0, 0, BRIGHTEN_DELTA,
        0, 1, 0, 0, BRIGHTEN_DELTA,
        0, 0, 1, 0, BRIGHTEN_DELTA,
        0, 0, 0, 1, 0]);
}
}
