package {

import mx.core.MovieClipAsset;
import flash.media.Sound;
import flash.media.SoundTransform;

public class RhinoShipType extends ShipType
{
    public function RhinoShipType () :void
    {
        super("Rhino", 1.75, 0.01, -0.005, 0.975, 0.825, 0.25, 1.5);
        ENGINE_MOV.gotoAndStop(2);
    }

    // Shooting sounds.
    [Embed(source="rsrc/ships/rhino/beam.mp3")]
    protected static var beamSound :Class;

    public const BEAM :Sound = Sound(new beamSound());

    [Embed(source="rsrc/ships/rhino/beam_tri.mp3")]
    protected static var triBeamSound :Class;

    public const TRI_BEAM :Sound = Sound(new triBeamSound());

    // Ship spawning
    [Embed(source="rsrc/ships/rhino/spawn.mp3")]
    protected static var spawnSound :Class;

    public const SPAWN :Sound = Sound(new spawnSound());

    // Looping sound - this is a movieclip to make the looping work without
    //  hiccups.  This is pretty hacky - we can't control the looping sound
    //  appropriately, so we just manipulate the volume.  So, the sounds are
    //  always running, just sometimes really quietly.  Bleh.

    // Engine hum.
    [Embed(source="rsrc/ships/rhino/engine_sound.swf#sound_main")]
    public static var engineSound :Class;

    public const ENGINE_MOV :MovieClipAsset =
        MovieClipAsset(new engineSound());

    // Animations
    [Embed(source="rsrc/ships/rhino/ship.swf#ship_movie_01_alt")]
    public const SHIP_ANIM :Class;

    [Embed(source="rsrc/ships/rhino/ship_shield.swf")]
    public const SHIELD_ANIM :Class;

    [Embed(source="rsrc/ships/rhino/ship_explosion_big.swf")]
    public const EXPLODE_ANIM :Class;

    [Embed(source="rsrc/ships/rhino/beam.swf")]
    public const SHOT_ANIM :Class;

}
}
