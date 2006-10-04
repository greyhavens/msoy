package {

import mx.core.BitmapAsset;

public class RobotFactory
{

    /**
     * Returns a BitmapAsset corresponding to the chunk of a robot requested.
     */
    public function getPart (
        color :int, partType :int, partStyle :int) :BitmapAsset
    {
        var partString :String = getPartString(color, partType, partStyle);
        var partClass :Class = this[partString];
        
        return BitmapAsset(new partClass());
    }

    /**
     * Generates a string identifying the name we've assigned to the requested
     * bit of artwork.
     */
    public function getPartString (
        color :int, partType :int, partStyle :int) :String
    {
        return "_part" + String(partType) +  String(partStyle) + String(color);
    }


    /** Slurp in all the robot parts. */

    [Embed(source="rsrc/robot/top/circle/red.png")]
    protected var _part100 :Class;
    [Embed(source="rsrc/robot/top/circle/green.png")]
    protected var _part101 :Class;
    [Embed(source="rsrc/robot/top/circle/blue.png")]
    protected var _part102 :Class;

    [Embed(source="rsrc/robot/top/square/red.png")]
    protected var _part110 :Class;
    [Embed(source="rsrc/robot/top/square/green.png")]
    protected var _part111 :Class;
    [Embed(source="rsrc/robot/top/square/blue.png")]
    protected var _part112 :Class;

    [Embed(source="rsrc/robot/top/triangle/red.png")]
    protected var _part120 :Class;
    [Embed(source="rsrc/robot/top/triangle/green.png")]
    protected var _part121 :Class;
    [Embed(source="rsrc/robot/top/triangle/blue.png")]
    protected var _part122 :Class;

    [Embed(source="rsrc/robot/middle/X/red.png")]
    protected var _part200 :Class;
    [Embed(source="rsrc/robot/middle/X/green.png")]
    protected var _part201 :Class;
    [Embed(source="rsrc/robot/middle/X/blue.png")]
    protected var _part202 :Class;

    [Embed(source="rsrc/robot/middle/Y/red.png")]
    protected var _part210 :Class;
    [Embed(source="rsrc/robot/middle/Y/green.png")]
    protected var _part211 :Class;
    [Embed(source="rsrc/robot/middle/Y/blue.png")]
    protected var _part212 :Class;

    [Embed(source="rsrc/robot/middle/Z/red.png")]
    protected var _part220 :Class;
    [Embed(source="rsrc/robot/middle/Z/green.png")]
    protected var _part221 :Class;
    [Embed(source="rsrc/robot/middle/Z/blue.png")]
    protected var _part222 :Class;

    [Embed(source="rsrc/robot/bottom/tank/red.png")]
    protected var _part300 :Class;
    [Embed(source="rsrc/robot/bottom/tank/green.png")]
    protected var _part301 :Class;
    [Embed(source="rsrc/robot/bottom/tank/blue.png")]
    protected var _part302 :Class;

    [Embed(source="rsrc/robot/bottom/legs/red.png")]
    protected var _part310 :Class;
    [Embed(source="rsrc/robot/bottom/legs/green.png")]
    protected var _part311 :Class;
    [Embed(source="rsrc/robot/bottom/legs/blue.png")]
    protected var _part312 :Class;

    [Embed(source="rsrc/robot/bottom/hover/red.png")]
    protected var _part320 :Class;
    [Embed(source="rsrc/robot/bottom/hover/green.png")]
    protected var _part321 :Class;
    [Embed(source="rsrc/robot/bottom/hover/blue.png")]
    protected var _part322 :Class;
}
}
