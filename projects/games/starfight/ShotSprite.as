package {

import flash.display.Sprite;

public class ShotSprite extends Sprite {

    /** Position. */
    public var boardX :Number;
    public var boardY :Number;

    /** Velocity. */
    public var xVel :Number;
    public var yVel :Number;

    public var complete :Boolean;

    public function ShotSprite (x :Number, y :Number,
        xVel :Number, yVel :Number) :void
    {
        boardX = x;
        boardY = y;
        this.xVel = xVel;
        this.yVel = yVel;

        complete = false;

        // TODO: Fancy image.
        graphics.beginFill(uint(0xFF0000));
        graphics.drawCircle(0, 0, 4);
    }

    public function tick (board :BoardSprite) :void
    {
        var coll :Collision = board.getCollision(boardX, boardY,
            boardX + xVel, boardY + yVel, COLLISION_RAD);
        if (coll != null) {
            boardX += xVel;
            boardY += yVel;
        } else {
            // TODO: Animate explosion if ship.
            complete = true;
        }
    }

    /**
     * Sets the sprite position for this ship based on its board pos and
     *  another pos which will be the center of the screen.
     */
    public function setPosRelTo (otherX :Number, otherY: Number) :void
    {
        x = ((boardX - otherX) * Codes.PIXELS_PER_TILE) + StarFight.WIDTH/2;
        y = ((boardY - otherY) * Codes.PIXELS_PER_TILE) + StarFight.HEIGHT/2;
    }

    protected static const COLLISION_RAD :Number = 0.1;
}
}
