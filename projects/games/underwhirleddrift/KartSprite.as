package {

import flash.display.Sprite;

import mx.core.MovieClipAsset;

public class KartSprite extends Sprite 
{
    public static const KART_LIGHT :String = "LightKart";
    public static const KART_MEDIUM :String = "MediumKart";

    public function KartSprite(kartType :String, stopFrame :int = 1)
    {
        try {
            _kart = new KartSprite[kartType]();
        } catch (re :ReferenceError) {
            throw new ArgumentError(kartType + " is not a recognized Kart Type");
        }
        if (stopFrame != -1) {
            _kart.gotoAndStop(stopFrame);
        }
        addChild(_kart);
        _kartType = kartType;
    }

    public function get kartType () :String
    {
        return _kartType;
    }

    /** light kart swf */
    [Embed(source='rsrc/lightkart.swf#kart')]
    protected static const LightKart :Class;

    /** medium kart swf */
    [Embed(source='rsrc/mediumkart.swf#kart')]
    protected static const MediumKart :Class;

    protected var _kart :MovieClipAsset;
     
    protected var _kartType :String;
}
}
