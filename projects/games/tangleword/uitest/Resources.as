package
{

import mx.core.BitmapAsset;

/** 
  Storage class for embedded resources; 
  later it may take over dynamic resources as well. 
*/

public class Resources
{

    // EMBEDDED RESOURCE ACCESSORS

    /** Returns a new instance of the default board image bitmap resource */
    public static function makeDefaultBoardImage () : BitmapAsset 
    {
        return new _defaultBoardImage ();
    }

    /** Returns a new instance of the default empty square bitmap resource */
    public static function makeDefaultEmptySquare () : BitmapAsset 
    {
        return new _defaultEmptySquare ();
    }


    // EMBEDDED RESOURCE DEFINITION

    [Embed(source="rsrc/background.png")]
    private static const _defaultBoardImage : Class;

    [Embed(source="rsrc/empty-square.png")]
    private static const _defaultEmptySquare : Class;

}


} // package
