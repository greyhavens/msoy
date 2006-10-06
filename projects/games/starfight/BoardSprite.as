package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.geom.Point;

import mx.core.MovieClipAsset;

public class BoardSprite extends Sprite
{
    public var boardWidth :int;
    public var boardHeight :int;
    
    public function BoardSprite (board :Board) :void
    {
        this.boardWidth = board.width;
        this.boardHeight = board.height;

        _obstacles = board.obstacles;
        paint();
    }

    /**
     * Sets the center of the screen.  We need to adjust ourselves to match.
     */
    public function setAsCenter (boardX :Number, boardY :Number) :void
    {
        x = StarFight.WIDTH/2 - (boardX*Codes.PIXELS_PER_TILE);
        y = StarFight.HEIGHT/2 - (boardY*Codes.PIXELS_PER_TILE);
    }

    /**
     * Returns the first collision for something of the specified radius (rad)
     *  moving along the given path.
     */
    public function getCollision (oldX :Number, oldY :Number,
        newX :Number, newY :Number, rad :Number) :Collision
    {
        var hits :Array = [];

        /** The first one we've seen so far. */
        var bestTime :Number = 1.0;
        var bestHit :Collision = null;

        var dx :Number = newX - oldX;
        var dy :Number = newY - oldY;

        // Check each obstacle and figure out which one we hit first.
        for each (var obs :Obstacle in _obstacles) {
            if ((obs.bX <= (newX + rad)) &&
                ((obs.bX+1.0) >= (newX - rad)) &&
                (obs.bY <= (newY + rad)) &&
                ((obs.bY+1.0) >= (newY - rad))) {

                // Find how long it is til our X coords collide.
                var timeToX :Number;
                if (dx > 0.0) {
                    timeToX = (obs.bX - (oldX+rad))/dx;
                } else if (dx < 0.0) {
                    timeToX = ((obs.bX+1.0) - (oldX-rad))/dx;
                } else {
                    timeToX = 0.0;
                }

                // Find how long it is til our Y coords collide.
                var timeToY :Number;
                if (dy > 0.0) {
                    timeToY = (obs.bY - (oldY+rad))/dy;
                } else if (dy < 0.0) {
                    timeToY = ((obs.bY+1.0) - (oldY-rad))/dy;
                } else {
                    timeToY = 0.0;
                }
                var time :Number = Math.max(timeToX, timeToY);
                
                if (time < bestTime) {
                    bestTime = time;
                    bestHit = new Collision(obs, time, timeToX > timeToY);
                }
            }
        }
        return bestHit;
    }

    /**
     * Returns a valid starting point for a ship which is clear.
     */
    public function getStartingPos () :Point
    {
        var pt :Point;
        while (true) {
            pt = new Point(Math.random() * (boardWidth - 4) + 2,
                Math.random() * (boardHeight - 4) + 2);
            if (getCollision(pt.x, pt.y, pt.x, pt.y, ShipSprite.COLLISION_RAD)
                == null) {
                return pt;
            }
        }

        // Should never reach here.
        return null;
    }

    /**
     * Draw the board.
     */
    public function paint () :void
    {
        var mask :Shape = new Shape();
        addChild(mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, boardWidth*Codes.PIXELS_PER_TILE,
            boardHeight*Codes.PIXELS_PER_TILE);
        mask.graphics.endFill();
        this.mask = mask;

        _spaceMovie = MovieClipAsset(new spaceAnim());
        _spaceMovie.gotoAndStop(1);
        addChild(_spaceMovie);

        for each (var obs :Obstacle in _obstacles) {
            addChild(obs);
        }
    }

    /** All the obstacles in the world. */
    protected var _obstacles :Array;

    /** Our ship animation. */
    protected var _spaceMovie :MovieClipAsset;

    [Embed(source="rsrc/space.swf")]
    protected var spaceAnim :Class;

}
}
