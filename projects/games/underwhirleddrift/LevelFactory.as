package {

import flash.events.Event;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.util.HashMap;

/**
 * Factory class to encapsulate all the available levels as embedded swfs.  
 */
public class LevelFactory 
{
    /**
     * create a Level that is returned, and later initialized.
     */
    public static function createLevel (level :int, ground :Ground) :Level
    {
        var instance :Level = new Level(ground);
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        // use an anonymous function in order to bind this level instance to it.
        loader.addEventListener(Event.COMPLETE, function (evt :Event) :void {
            instance.initialize(loader.getClass("background"), loader.getClass("rough"), 
                loader.getClass("track"), loader.getClass("wall"), new LevelConfig(
                loader.getClass("objects"), OBJECT_MAPPING), loader.getClass("objects"));
        });
        try {
            loader.load(new LevelFactory["LEVEL_" + level]());
        } catch (re :ReferenceError) {
            return null;
        }
        return instance;
    }

    /** the list of level swfs.  This will probably get pretty long */
    [Embed(source='rsrc/level_0.swf', mimeType='application/octet-stream')]
    protected static const LEVEL_0 :Class;

    /** the objects - this is done statically for now - may or may not switch to using the loader
     * in the future */
    [Embed(source='rsrc/objects.swf#column')]
    protected static const COLUMN :Class;
    [Embed(source='rsrc/objects.swf#column')]
    protected static const TREE :Class;
    [Embed(source='rsrc/objects.swf#dora_box')]
    protected static const DORA_BOX :Class;
    // initialize the mapping of colors to objects
    protected static var OBJECT_MAPPING :HashMap;
    private static function staticInit () :void
    {
        OBJECT_MAPPING = new HashMap();
        OBJECT_MAPPING.put(0xFF0000, { cls: null, type: LevelConfig.OBJECT_STARTING_LINE_POINT });
        OBJECT_MAPPING.put(0xFFFFFF, { cls: null, type: LevelConfig.OBJECT_STARTING_POSITION });
        OBJECT_MAPPING.put(0x00FFFF, { cls: COLUMN, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0xFFFF00, { cls: DORA_BOX, type: LevelConfig.OBJECT_BONUS });
        OBJECT_MAPPING.put(0x00FF00, { cls: TREE, type: LevelConfig.OBJECT_OBSTACLE });
        
    }
    staticInit();
}
}
