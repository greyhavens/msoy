package {

import flash.media.Sound;

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

    // Ship explosion
    [Embed(source="rsrc/explode.mp3")]
    protected static var explodeSound :Class;

    public static const SHIP_EXPLODE :Sound = Sound(new explodeSound());

    // Powerup
    [Embed(source="rsrc/powerup.mp3")]
    protected static var powerupSound :Class;

    public static const POWERUP :Sound = Sound(new powerupSound());

}
}
