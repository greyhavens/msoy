package {

import flash.media.Sound;
import flash.media.SoundTransform;

import mx.core.MovieClipAsset;

/**
 * Contains all the sounds we want to play.
 *  A note on organization - bucking the normal to keep sounds with their associated
 *  embedded sound class.
 */
public class Sounds {

    // Shooting sounds.
    [Embed(source="rsrc/beam.mp3")]
    protected static var beamSound :Class;

    public static const BEAM :Sound = Sound(new beamSound());

    [Embed(source="rsrc/beam_tri.mp3")]
    protected static var triBeamSound :Class;

    public static const TRI_BEAM :Sound = Sound(new triBeamSound());

    // Shooting sounds.
    [Embed(source="rsrc/beam2.mp3")]
    protected static var beam2Sound :Class;

    public static const BEAM2 :Sound = Sound(new beam2Sound());

    [Embed(source="rsrc/beam2_tri.mp3")]
    protected static var triBeam2Sound :Class;

    public static const TRI_BEAM2 :Sound = Sound(new triBeam2Sound());

    // Various small explosions/blast hits.
    [Embed(source="rsrc/ship_hit.mp3")]
    protected static var shipHitSound :Class;

    public static const SHIP_HIT :Sound = Sound(new shipHitSound());

    [Embed(source="rsrc/shields_hit.mp3")]
    protected static var shieldsHitSound :Class;

    public static const SHIELDS_HIT :Sound = Sound(new shieldsHitSound());

    [Embed(source="rsrc/asteroid_hit.mp3")]
    protected static var asteroidHitSound :Class;

    public static const ASTEROID_HIT :Sound = Sound(new asteroidHitSound());

    [Embed(source="rsrc/metal_hit.mp3")]
    protected static var metalHitSound :Class;

    public static const METAL_HIT :Sound = Sound(new metalHitSound());

    [Embed(source="rsrc/junk_hit.mp3")]
    protected static var junkHitSound :Class;

    public static const JUNK_HIT :Sound = Sound(new junkHitSound());

    // Collisions
    [Embed(source="rsrc/collision_junk.mp3")]
    protected static var junkCollSound :Class;

    public static const JUNK_COLLIDE :Sound = Sound(new junkCollSound());

    [Embed(source="rsrc/collision_asteroid.mp3")]
    protected static var asteroidCollSound :Class;

    public static const ASTEROID_COLLIDE :Sound =
        Sound(new asteroidCollSound());

    [Embed(source="rsrc/collision_metal.mp3")]
    protected static var metalCollSound :Class;

    public static const METAL_COLLIDE :Sound = Sound(new metalCollSound());

    [Embed(source="rsrc/collision_zappy.mp3")]
    protected static var zappyCollSound :Class;

    public static const ZAPPY_COLLIDE :Sound = Sound(new zappyCollSound());


    // Ship explosion
    [Embed(source="rsrc/explode.mp3")]
    protected static var explodeSound :Class;

    public static const SHIP_EXPLODE :Sound = Sound(new explodeSound());

    // Powerup
    [Embed(source="rsrc/powerup.mp3")]
    protected static var powerupSound :Class;

    public static const POWERUP :Sound = Sound(new powerupSound());

    // Looping sounds - these are movieclips to make the looping work without
    //  hiccups.  This is pretty hacky - we can't control the looping sound
    //  appropriately, so we just manipulate the volume.  So, the sounds are
    //  always running, just sometimes really quietly.  Bleh.
    [Embed(source="rsrc/engine_sound.swf")]
    public static var engineSound :Class;

    public static const ENGINE_MOV :MovieClipAsset =
        MovieClipAsset(new engineSound());

    [Embed(source="rsrc/engine2_sound.swf")]
    public static var engine2Sound :Class;

    public static const ENGINE2_MOV :MovieClipAsset =
        MovieClipAsset(new engine2Sound());

    [Embed(source="rsrc/shields_sound.swf")]
    protected static var shieldsSound :Class;

    public static const SHIELDS_MOV :MovieClipAsset =
        MovieClipAsset(new shieldsSound());

    [Embed(source="rsrc/thruster_sound.swf")]
    protected static var thrusterSound :Class;

    public static const THRUSTER_MOV :MovieClipAsset =
        MovieClipAsset(new thrusterSound());

    [Embed(source="rsrc/thruster_retro_sound.swf")]
    protected static var thrusterRetroSound :Class;

    public static const THRUSTER_RETRO_MOV :MovieClipAsset =
        MovieClipAsset(new thrusterRetroSound());

    // Static initialization - make all of our looping sounds silent initially.
    {
        ENGINE_MOV.soundTransform = Sounds.OFF;
        ENGINE2_MOV.soundTransform = Sounds.OFF;
        SHIELDS_MOV.soundTransform = Sounds.OFF;
        THRUSTER_MOV.soundTransform = Sounds.OFF;
        THRUSTER_RETRO_MOV.soundTransform = Sounds.OFF;
    }

    /** Volume constants - we use these to turn our looping sounds on/off. */
    public static const OFF :SoundTransform = new SoundTransform(0);
    public static const ON :SoundTransform = new SoundTransform(1);
}
}
