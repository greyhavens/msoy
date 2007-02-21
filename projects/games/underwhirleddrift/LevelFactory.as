package {

import flash.events.Event;

import com.threerings.util.EmbeddedSwfLoader;

/**
 * Factory class to encapsulate all the available levels as embedded swfs.  
 */
public class LevelFactory 
{
    /**
     * create a Level that is returned, and later initialized.
     */
    public static function createLevel (level :int) :Level
    {
        var instance :Level = new Level();
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        // use an anonymous function in order to bind this level instance to it.
        loader.addEventListener(Event.COMPLETE, function (evt :Event) :void {
            var background :Class = loader.getSymbol("background") as Class;
            var rough :Class = loader.getSymbol("rough") as Class;
            var track :Class = loader.getSymbol("track") as Class;
            var wall :Class = loader.getSymbol("wall") as Class;
            instance.initialize(background, rough, track, wall);
        });
        loader.load(new LEVELS[level]);
        return instance;
    }

    /** the list of level swfs.  This will probably get pretty long */
    [Embed(source='rsrc/level_1.swf', mimeType='application/octet-stream')]
    protected static const LEVEL_1 :Class;
    protected static const LEVELS :Array = [ LEVEL_1 ];
}
}
