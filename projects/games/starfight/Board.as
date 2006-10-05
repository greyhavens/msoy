package {

import flash.utils.ByteArray;

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
            var obs :Obstacle = new Obstacle(0, 0, 0);
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
        for (ii = 0; ii < 50; ii++) {
            obstacles.push(new Obstacle(Obstacle.WALL,
                Math.random()*width, Math.random()*height));
        }
        
        // Place a wall around the outside of the board.

        for (ii = 0; ii < height; ii++) {
            obstacles.push(new Obstacle(Obstacle.WALL, 0, ii));
            obstacles.push(new Obstacle(Obstacle.WALL, width-1, ii));
        }

        for (ii = 0; ii < width; ii++) {
            obstacles.push(new Obstacle(Obstacle.WALL, ii, 0));
            obstacles.push(new Obstacle(Obstacle.WALL, ii, height-1));
        }
    }
}
}
