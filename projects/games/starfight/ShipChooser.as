package {

import flash.display.Sprite;
import flash.events.MouseEvent;

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

public class ShipChooser extends Sprite
{
    public function ShipChooser(game :StarFight)
    {

        // Partially obscure background.
        alpha = 0.5;
        graphics.beginFill(Codes.BLACK);
        graphics.drawRect(0, 0, StarFight.WIDTH, StarFight.HEIGHT);
        graphics.endFill();
        graphics.lineStyle(2, Codes.CYAN);
        graphics.drawRoundRect((StarFight.WIDTH - SPACING * (Codes.SHIP_TYPES.length+1))/2,
            StarFight.HEIGHT/2 - SPACING, SPACING * (Codes.SHIP_TYPES.length+1), 2 * SPACING, 10.0, 10.0);
        _game = game;

        var format:TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = Codes.CYAN;
        format.size = 16;
        format.bold = true;

        var selectText :TextField = new TextField();
        selectText.autoSize = TextFieldAutoSize.CENTER;
        selectText.selectable = false;
        selectText.x = StarFight.WIDTH/2;
        selectText.y = StarFight.HEIGHT/2 - SPACING;
        selectText.defaultTextFormat = format;
        selectText.text = "Select Your Ship";
        addChild(selectText);

        for (var ii :int = 0; ii < Codes.SHIP_TYPES.length; ii++) {
            var type :ShipType = Codes.SHIP_TYPES[ii];
            addButton(type, ii, Codes.SHIP_TYPES.length);
        }
    }

    /**
     * Adds a ship button.
     */
    protected function addButton (type :ShipType, idx :int, total :int) :void
    {
        var ship :ShipSprite = new ShipSprite(null, null, true, -1, type.name, false);
        ship.pointUp();
        ship.setShipType(idx);
        ship.addEventListener(MouseEvent.CLICK, chooseHandler);
        ship.addEventListener(MouseEvent.MOUSE_OVER, mouseOverHandler);
        ship.addEventListener(MouseEvent.MOUSE_OUT, mouseOutHandler);

        ship.x = StarFight.WIDTH/2 + SPACING * (idx - (total-1)/2.0);
        ship.y = StarFight.HEIGHT/2;

        addChild(ship);
    }

    public function chooseHandler (event :MouseEvent) :void
    {
        choose((event.currentTarget as ShipSprite).shipType);

        // prevent NPE on further click event handlers
        event.stopImmediatePropagation();
    }

    public function mouseOverHandler (event :MouseEvent) :void
    {
        (event.currentTarget as ShipSprite).setAnimMode(ShipSprite.FORWARD, false);
        event.currentTarget.scaleX = HIGHLIGHT_SCALE;
        event.currentTarget.scaleY = HIGHLIGHT_SCALE;
    }

    public function mouseOutHandler (event :MouseEvent) :void
    {
        (event.currentTarget as ShipSprite).setAnimMode(ShipSprite.IDLE, false);
        event.currentTarget.scaleX = 1.0;
        event.currentTarget.scaleY = 1.0;
    }

    /**
     * Register a user choice of a specific ship.
     */
    public function choose (typeIdx :int) :void
    {
        _game.chooseShip(typeIdx);
        _game.removeChild(this);
    }

    protected static const SPACING :int = 60;
    protected static const HIGHLIGHT_SCALE :Number = 1.2;

    protected var _game :StarFight;
}
}
