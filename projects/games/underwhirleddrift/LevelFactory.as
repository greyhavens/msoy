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
        // TEMP!!!!
        level = 1;

        var instance :Level = new Level(ground);
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        // use an anonymous function in order to bind this level instance to it.
        loader.addEventListener(Event.COMPLETE, function (evt :Event) :void {
            instance.initialize(loader.getClass("background"), loader.getClass("rough"), 
                loader.getClass("track"), loader.getClass("wall"), new LevelConfig(
                loader.getClass("objects"), OBJECT_MAPPING));
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
    [Embed(source='rsrc/level_1.swf', mimeType='application/octet-stream')]
    protected static const LEVEL_1 :Class;

    /** the bonuses */
    [Embed(source='rsrc/objects.swf#dora_box')]
    protected static const DORA_BOX :Class;

    /** the obstacles */
    [Embed(source='rsrc/objects.swf#column')]
    protected static const COLUMN :Class;
    [Embed(source='rsrc/objects.swf#column_broken')]
    protected static const COLUMN_BROKEN :Class;
    [Embed(source='rsrc/objects.swf#gold_tree')]
    protected static const GOLD_TREE :Class;
    [Embed(source='rsrc/objects.swf#green_tree')]
    protected static const GREEN_TREE :Class;
    [Embed(source='rsrc/objects.swf#red_tree')]
    protected static const RED_TREE :Class;

    // initialize the mapping of colors to objects
    protected static var OBJECT_MAPPING :HashMap;
    private static function staticInit () :void
    {
        OBJECT_MAPPING = new HashMap();
        OBJECT_MAPPING.put(0xFF0000, { cls: null, type: LevelConfig.OBJECT_STARTING_LINE_POINT });
        OBJECT_MAPPING.put(0xFFFFFF, { cls: null, type: LevelConfig.OBJECT_STARTING_POSITION });
        OBJECT_MAPPING.put(0xFF00FF, { cls: null, type: LevelConfig.OBJECT_BOOST });
        OBJECT_MAPPING.put(0x00FF00, { cls: null, type: LevelConfig.OBJECT_JUMP_RAMP });

        OBJECT_MAPPING.put(0xFFFF00, { cls: DORA_BOX, type: LevelConfig.OBJECT_BONUS });

        OBJECT_MAPPING.put(0xED145A, { cls: COLUMN, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x9D0039, { cls: COLUMN_BROKEN, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x662D91, { cls: GOLD_TREE, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x00ADEF, { cls: GREEN_TREE, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0xA36109, { cls: RED_TREE, type: LevelConfig.OBJECT_OBSTACLE });
    }
    staticInit();
}
}
