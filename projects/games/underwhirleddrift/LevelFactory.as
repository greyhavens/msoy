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
            new LevelConfig(loader.getClass("objects"), OBJECT_MAPPING);
            instance.initialize(loader.getClass("background"), loader.getClass("rough"), 
                loader.getClass("track"), loader.getClass("wall")); 
        });
        loader.load(new LEVELS[level]);
        return instance;
    }

    /** the list of level swfs.  This will probably get pretty long */
    [Embed(source='rsrc/level_1.swf', mimeType='application/octet-stream')]
    protected static const LEVEL_1 :Class;
    protected static const LEVELS :Array = [ LEVEL_1 ];

    /** the objects - this is done statically for now - may or may not switch to using the loader
     * in the future */
    [Embed(source='rsrc/objects.swf#column')]
    protected static const COLUMN :Class;
    [Embed(source='rsrc/objects.swf#dora_box')]
    protected static const DORA_BOX :Class;
    protected static const OBJECT_MAPPING :Object = {
        finishLineColor: 0xff0000,
        startingPointColor: 0xffffff,
        obstacles: [ 
            { color: 0x00ffff, cls: COLUMN } ],
        bonuses: [
            { color: 0xffff00, cls: DORA_BOX } ]
    };
}
}
