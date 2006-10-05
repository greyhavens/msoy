package {

import flash.display.Sprite;

import flash.geom.Point;

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
        x = StarFight.WIDTH - (boardX*Codes.PIXELS_PER_TILE);
        y = StarFight.HEIGHT - (boardY*Codes.PIXELS_PER_TILE);
    }

    /**
     * Returns the first collision for a ship moving along the given path.
     */
    public function getCollision (oldX :Number, oldY :Number,
        newX :Number, newY :Number) :Collision
    {
        var hits :Array = [];

        /** The first one we've seen so far. */
        var bestTime :Number = 1.0;
        var bestHit :Collision = null;

        var dx :Number = newX - oldX;
        var dy :Number = newY - oldY;

        // Check each obstacle and figure out which one we hit first.
        for each (var obs :Obstacle in _obstacles) {
            if ((obs.x <= (newX + 1.0)) &&
                ((obs.x+1.0) >= (newX - 1.0)) &&
                (obs.y <= (newY + 1.0)) &&
                ((obs.y+1.0) >= (newY - 1.0))) {

                // Find how long it is til our X coords collide.
                var timeToX :Number;
                if (dx > 0.0) {
                    timeToX = (obs.x - (oldX+1.0))/dx;
                } else if (dx < 0.0) {
                    timeToX = ((obs.x+1.0) - (oldX-1.0))/dx;
                } else {
                    timeToX = 0.0;
                }

                // Find how long it is til our Y coords collide.
                var timeToY :Number;
                if (dy > 0.0) {
                    timeToY = (obs.y - (oldY+1.0))/dy;
                } else if (dy < 0.0) {
                    timeToY = ((obs.y+1.0) - (oldY-1.0))/dy;
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
            if (getCollision(pt.x, pt.y, pt.x, pt.y) == null) {
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
        graphics.beginFill(BLACK);
        graphics.drawRect(-StarFight.WIDTH/2, - StarFight.HEIGHT/2,
            boardWidth*Codes.PIXELS_PER_TILE,
            boardHeight*Codes.PIXELS_PER_TILE);

        for each (var obs :Obstacle in _obstacles) {
            obs.paint(graphics);
        }
    }

    /** All the obstacles in the world. */
    protected var _obstacles :Array;

    /** Color constants. */
    protected static const BLACK :uint = uint(0x000000);
}
}
