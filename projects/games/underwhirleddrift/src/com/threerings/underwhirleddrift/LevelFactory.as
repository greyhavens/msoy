package com.threerings.underwhirleddrift {

import com.threerings.util.HashMap;

/**
 * Factory class to encapsulate all the available levels as embedded swfs.  
 */
public class LevelFactory 
{
    public static const TOTAL_LEVELS :int = 3;

    public static function createLevel (level :int, ground :Ground) :Level
    {
        try {
            return new Level(ground, LevelFactory["BACKGROUND_" + level],
                LevelFactory["ROUGH_" + level], LevelFactory["TRACK_" + level],
                LevelFactory["WALL_" + level], LevelFactory["HORIZON_" + level],
                new LevelConfig(LevelFactory["OBJECTS_" + level], OBJECT_MAPPING));
        } catch (re :ReferenceError) {
            Log.getLog(LevelFactory).debug("Failed attempt to create level " + level);
        }
        return null;
    }

    /******* Master List of Level SWFs.  When we get alot of these, the issue surrounding
     * Application Domains in EmbeddedSwfLoader should be figured out, so that we don't have to 
     * list a ton of class per level */

    /** Backgrounds */
    [Embed(source='../../../../rsrc/level_0.swf#background')]
    protected static const BACKGROUND_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#background')]
    protected static const BACKGROUND_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#background')]
    protected static const BACKGROUND_2 :Class;

    /** Roughs */
    [Embed(source='../../../../rsrc/level_0.swf#rough')]
    protected static const ROUGH_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#rough')]
    protected static const ROUGH_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#rough')]
    protected static const ROUGH_2 :Class;

    /** Tracks */
    [Embed(source='../../../../rsrc/level_0.swf#track')]
    protected static const TRACK_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#track')]
    protected static const TRACK_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#track')]
    protected static const TRACK_2 :Class;
    
    /** Walls */
    [Embed(source='../../../../rsrc/level_0.swf#wall')]
    protected static const WALL_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#wall')]
    protected static const WALL_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#wall')]
    protected static const WALL_2 :Class;

    /** Object Layers */
    [Embed(source='../../../../rsrc/level_0.swf#objects')]
    protected static const OBJECTS_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#objects')]
    protected static const OBJECTS_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#objects')]
    protected static const OBJECTS_2 :Class;

    /** Horizon images */
    [Embed(source='../../../../rsrc/level_0.swf#horizon')]
    protected static const HORIZON_0 :Class;
    [Embed(source='../../../../rsrc/level_1.swf#horizon')]
    protected static const HORIZON_1 :Class;
    [Embed(source='../../../../rsrc/level_2.swf#horizon')]
    protected static const HORIZON_2 :Class;

    /****** Magic fun scenery props from the objects.swf ******/

    /** the bonuses */
    [Embed(source='../../../../rsrc/objects.swf#dora_box')]
    protected static const DORA_BOX :Class;
    [Embed(source='../../../../rsrc/objects.swf#soul_well')]
    protected static const SOUL_WELL :Class;

    /** the obstacles */
    [Embed(source='../../../../rsrc/objects.swf#column')]
    protected static const COLUMN :Class;
    [Embed(source='../../../../rsrc/objects.swf#column_broken')]
    protected static const COLUMN_BROKEN :Class;
    [Embed(source='../../../../rsrc/objects.swf#gold_tree')]
    protected static const GOLD_TREE :Class;
    [Embed(source='../../../../rsrc/objects.swf#green_tree')]
    protected static const GREEN_TREE :Class;
    [Embed(source='../../../../rsrc/objects.swf#red_tree')]
    protected static const RED_TREE :Class;
    [Embed(source='../../../../rsrc/objects.swf#lava_stalagmite')]
    protected static const LAVA_STALAGMITE :Class;

    // initialize the mapping of colors to objects
    protected static var OBJECT_MAPPING :HashMap;
    private static function staticInit () :void
    {
        OBJECT_MAPPING = new HashMap();
        OBJECT_MAPPING.put(0xFF0000, { cls: null, type: LevelConfig.OBJECT_STARTING_LINE_POINT });
        OBJECT_MAPPING.put(0xFFFFFF, { cls: null, type: LevelConfig.OBJECT_STARTING_POSITION });
        OBJECT_MAPPING.put(0x00FF00, { cls: null, type: LevelConfig.OBJECT_JUMP_RAMP });
        OBJECT_MAPPING.put(0x00FFFF, { cls: null, type: LevelConfig.OBJECT_BOOST_POINT_EAST });
        OBJECT_MAPPING.put(0x790000, { cls: null, type: LevelConfig.OBJECT_BOOST_POINT_WEST });
        OBJECT_MAPPING.put(0xFF00FF, { cls: null, type: LevelConfig.OBJECT_BOOST_POINT_NORTH });
        OBJECT_MAPPING.put(0x0000FF, { cls: null, type: LevelConfig.OBJECT_BOOST_POINT_SOUTH });

        OBJECT_MAPPING.put(0xFFFF00, { cls: DORA_BOX, type: LevelConfig.OBJECT_BONUS });
        OBJECT_MAPPING.put(0xF7941D, { cls: SOUL_WELL, type: LevelConfig.OBJECT_BONUS });

        OBJECT_MAPPING.put(0xED145A, { cls: COLUMN, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x9D0039, { cls: COLUMN_BROKEN, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x662D91, { cls: GOLD_TREE, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x00ADEF, { cls: GREEN_TREE, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0xA36109, { cls: RED_TREE, type: LevelConfig.OBJECT_OBSTACLE });
        OBJECT_MAPPING.put(0x005e20, { cls: LAVA_STALAGMITE, type: LevelConfig.OBJECT_OBSTACLE });
    }
    staticInit();
}
}
