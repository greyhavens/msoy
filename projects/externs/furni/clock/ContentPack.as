package {

public class ContentPack extends Object
{
    /** A Bitmap class containing the clock face. */
    [Embed(source="face.png")]
    public var face :Class;

    /** An array containing the coordinate of the point, in face coordinates,
     * at which the hands attach to the face. */
    public var faceCenter :Array = [ 100, 100 ];

    /** A Bitmap class containing the hour hand. */
    [Embed(source="hour_hand.png")]
    public var hourHand :Class;

    /** An array containing the coordinate of the point, in the hand's
     * coordinates, at which the hour hand connects to the face. */
    public var hourPoint :Array = [ 10, 50 ];

    /** A Bitmap class containing the hour hand. */
    [Embed(source="minute_hand.png")]
    public var minuteHand :Class;

    /** An array containing the coordinate of the point, in the hand's
     * coordinates, at which the minute hand connects to the face. */
    public var minutePoint :Array = [ 10, 100 ];

    /** [Optional] A Bitmap class containing the hour hand. */
    [Embed(source="second_hand.png")]
    public var secondHand :Class;

    /** An array containing the coordinate of the point, in the hand's
     * coordinates, at which the second hand connects to the face. */
    public var secondPoint :Array = [ 1, 100 ];

    /** Whether we have a smooth or 'ticking' seconds hand. */
    public var smoothSeconds :Boolean = false;
}
}
