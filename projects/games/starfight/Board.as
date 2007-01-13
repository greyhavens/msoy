package {

import flash.utils.ByteArray;

/**
 * Data structure for the board.
 */
public class Board
{
    /** Board size in tiles. */
    public var width :int;
    public var height :int;

    /** All the obstacles on the board. */
    public var obstacles :Array;

    /**
     * Constructs a brand new board.
     */
    public function Board (width :int, height :int, initObstacles :Boolean)
    {
        this.width = width;
        this.height = height;

        if (initObstacles) {
            loadObstacles();
        }
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        width = bytes.readInt();
        height = bytes.readInt();
        
        obstacles = [];
        while (bytes.bytesAvailable > 0) {
            var obs :Obstacle = new Obstacle(0, 0, 0, false);
            obs.readFrom(bytes);
            obstacles.push(obs);
        }
    }

    /**
     * Serialize our data to a byte array.
     */
    public function writeTo (bytes :ByteArray) :ByteArray
    {
        bytes.writeInt(width);
        bytes.writeInt(height);
        for each (var obs :Obstacle in obstacles) {
            obs.writeTo(bytes);
        }

        return bytes;
    }

    /**
     * Loads all the obstacles in the world.
     */
    protected function loadObstacles () :void
    {
        obstacles = [];

        var ii :int;

        // TODO Load obstacles from a file instead of random.
        var numAsteroids :int = width*height/100;
        for (ii = 0; ii < numAsteroids; ii++) {
            var type :int = 0;
            switch (int(Math.floor(Math.random()*2.0))) {
            case 0: type = Obstacle.ASTEROID_1; break;
            case 1: type = Obstacle.ASTEROID_2; break;
            }
            obstacles.push(new Obstacle(type,
                Math.random()*width, Math.random()*height, true));
        }
        
        // Place a wall around the outside of the board.

        for (ii = 0; ii < height; ii++) {
            obstacles.push(new Obstacle(Obstacle.WALL, 0, ii, true));
            obstacles.push(new Obstacle(Obstacle.WALL, width-1, ii, true));
        }

        for (ii = 0; ii < width; ii++) {
            obstacles.push(new Obstacle(Obstacle.WALL, ii, 0, true));
            obstacles.push(new Obstacle(Obstacle.WALL, ii, height-1, true));
        }
    }
}
}
