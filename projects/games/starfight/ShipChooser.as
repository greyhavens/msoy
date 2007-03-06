package {

import flash.display.Sprite;
import flash.events.MouseEvent;

public class ShipChooser extends Sprite
{
    public function ShipChooser(game :StarFight)
    {
        _game = game;

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
        ship.setShipType(idx);
        ship.addEventListener(MouseEvent.CLICK, chooseHandler);

        addChild(ship);
    }

    public function chooseHandler (event :MouseEvent) :void
    {
        choose((event.currentTarget as ShipSprite).shipType);
    }

    /**
     * Register a user choice of a specific ship.
     */
    public function choose (typeIdx :int) :void
    {
        _game.removeChild(this);
        _game.chooseShip(typeIdx);
    }

    protected var _game :StarFight;
}
}
