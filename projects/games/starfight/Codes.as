package {

public class Codes {
    public static const PIXELS_PER_TILE :int = 20;
    public static const BG_PIXELS_PER_TILE :int = 5;

    public static const RADS_TO_DEGS :Number = 180.0/Math.PI;
    public static const DEGS_TO_RADS :Number = Math.PI/180.0;

    /** Color constants. */
    public static const BLACK :uint = uint(0x000000);
    public static const CYAN :uint = uint(0x00FFFF);

    /** millis between screen refreshes. */
    public static const REFRESH_RATE :int = 50;

    /** How often we send updates to the server. */
    public static const FRAMES_PER_UPDATE :int = 3;

    /** The different available types of ships. */
    public static const SHIP_TYPES :Array = [
        new WaspShipType(),
        new RhinoShipType()
    ]

}
}
