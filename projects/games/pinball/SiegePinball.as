package {

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

import org.cove.flade.util.*;
import org.cove.flade.surfaces.*;
import org.cove.flade.constraints.*;
import org.cove.flade.primitives.*;
import org.cove.flade.DynamicsEngine;

import flash.display.*;
import flash.text.*;
import flash.events.*;
import flash.ui.*;
import flash.utils.*;

import mx.core.SoundAsset;

[SWF(width="640", height="480")]
public class SiegePinball extends Sprite
    implements Game, PropertyChangedListener, MessageReceivedListener
{
    protected var engine:DynamicsEngine;

    public function SiegePinball () {
        graphics.beginFill(0xEEDDCC);
        graphics.drawRect(0, 0, 640, 580);

        engine = new DynamicsEngine(this);

        engine.setDamping(0.999);
        engine.setGravity(0.0, 0.04);
        engine.setSurfaceBounce(1.0);
        engine.setSurfaceFriction(0.1);
        engine.coeffPinball = 8;

        // platform
//        engine.addSurface(new LineSurface(535, 5, 635, 5));
        engine.addSurface(new LineSurface(635, 475, 635, 5));
//        engine.addSurface(new LineSurface(635, 475, 5, 475));
        engine.addSurface(new LineSurface(5, 5, 5, 475));

        var sounds :Array = new Array();
        sounds.push(SoundAsset(new _tone()));
        sounds.push(SoundAsset(new _spoon()));
        sounds.push(SoundAsset(new _bike()));
        sounds.push(SoundAsset(new _spittoon()));

        var lastBeep :int = -1;
        // circles
        var soundPerTile:Array = new Array();
        for (var i :int = 0; i < 6; i ++) {
            var tile :Circle = new Circle(
                120 + Math.random() * 380,
                140 + Math.random() * 240,
                40 + Math.random() * 20);
            tile.color = Math.random() * 0x1000000;
            engine.addSurface(tile);
            tile.sound = 
                sounds[Math.floor(Math.random()*sounds.length)];
            tile.onContactListener = function(contact: AbstractTile) :void {
                var now :int = getTimer();
                if (now - lastBeep > 100) {
                    (contact as Circle).sound.play();
                    lastBeep = now;
                }
            }
        }
        engine.paintSurfaces();
        cow = new Cow(engine, 580, 380);
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;

        addEventListener(Event.ENTER_FRAME, enterFrame);
        this.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDown);
        this.stage.addEventListener(KeyboardEvent.KEY_UP, keyUp);

        resetCow();
    }

    public function enterFrame (event:Event) :void {
        run();
    }

    protected var leftKey :Boolean = false;
    protected var rightKey :Boolean = false;
    protected var downKey :Boolean = false;
    protected var cow:Cow;
    protected var cowPicture:Bitmap;
    protected var cowBackLeg:Bitmap;
    protected var cowFrontLeg:Bitmap;

    protected var cowAngle:Number = 0;
    protected var backLegAngle:Number = 0;
    protected var frontLegAngle:Number = 0;

    public function keyDown (event:KeyboardEvent) :void {
        if (event.keyCode == Keyboard.LEFT) {
            leftKey = true;
        } else if (event.keyCode == Keyboard.RIGHT) {
            rightKey = true;
        } else if (event.keyCode == Keyboard.DOWN) {
            downKey = true;
        }
    }

    public function keyUp (event:KeyboardEvent) :void {
        if (event.keyCode == Keyboard.LEFT) {
            leftKey = false;
        } else if (event.keyCode == Keyboard.RIGHT) {
            rightKey = false;
        } else if (event.keyCode == Keyboard.DOWN) {
            fireCow();
        }
    }

    protected function resetCow () :void
    {
        cowAngle = 0;
        backLegAngle = 0;
        frontLegAngle = 0;
        launched = false;
        if (cowPicture == null) {
            cowPicture = new _cowWithoutLegs();
            this.addChild(cowPicture);
            cowBackLeg = new _cowBackLeg();
            this.addChild(cowBackLeg);
            cowFrontLeg = new _cowFrontLeg();
            this.addChild(cowFrontLeg);
            var scale :Number =
                cow.p1.curr.distance(cow.p0.curr) / cowPicture.bitmapData.width;
            cowPicture.scaleX = scale;
            cowPicture.scaleY = scale;
            cowBackLeg.scaleX = scale;
            cowBackLeg.scaleY = scale;
            cowFrontLeg.scaleX = scale;
            cowFrontLeg.scaleY = scale;
        }
        leftKey = false;
        rightKey = false;
        downKey = false;
    }

    protected function fireCow () :void {
        backLegAngle = 0;
        frontLegAngle = 0;
        cow.launch(cowAngle + Math.PI, 6);
        launched = true;
    }
    protected var launched :Boolean;

    public function run () :void {
        if (cow.p0.curr.y > 500) {
            resetCow();
        }
        if (leftKey) {
            cowAngle += 0.03;
        } else if (rightKey) {
            cowAngle -= 0.03;
        }
//        if (downKey) {
//            frontLegAngle += 0.03;
//            backLegAngle += 0.03;
//        }
        engine.timeStep();
        engine.timeStep();
        engine.timeStep();
        engine.timeStep();
        if (!launched) {
            cow.reconfigureCow(580, 380, cowAngle);
        } else {
            cowAngle = cow.p1.curr.minusNew(cow.p0.curr).angle();
        }
            frontLegAngle =
                cow.leg0.curr.minusNew(cow.p3.curr).angle() + Math.PI/2;
            frontLegAngle -= cowAngle;
            backLegAngle =
                cow.leg1.curr.minusNew(cow.p2.curr).angle() + Math.PI/2;
            backLegAngle -= cowAngle;
        cowPicture.x = cow.p0.curr.x;
        cowPicture.y = cow.p0.curr.y;
        cowPicture.rotation = -180 * cowAngle / Math.PI;
        cowBackLeg.x =
            .15*cow.p0.curr.x + .35*cow.p1.curr.x +
            .35*cow.p2.curr.x + .15*cow.p3.curr.x;
        cowBackLeg.y =
            .15*cow.p0.curr.y + .35*cow.p1.curr.y +
            .35*cow.p2.curr.y + .15*cow.p3.curr.y;
        cowBackLeg.rotation = -180 * (cowAngle+backLegAngle) / Math.PI;
        cowFrontLeg.x =
            .35*cow.p0.curr.x + .15*cow.p1.curr.x +
            .15*cow.p2.curr.x + .35*cow.p3.curr.x;
        cowFrontLeg.y =
            .35*cow.p0.curr.y + .15*cow.p1.curr.y +
            .15*cow.p2.curr.y + .35*cow.p3.curr.y;
        cowFrontLeg.rotation = -180 * (cowAngle+frontLegAngle) / Math.PI;

        engine.paintPrimitives();
        engine.paintConstraints();          
    }       
    protected var _gameObject :EZGame;

    [Embed(source="Tone.mp3")]
    protected static const _tone :Class;

    [Embed(source="Spoon.mp3")]
    protected static const _spoon :Class;

    [Embed(source="Bike.mp3")]
    protected static const _bike :Class;

    [Embed(source="Spittoon.mp3")]
    protected static const _spittoon :Class;

    [Embed(source="alpha-cow-no-legs.png")]
    protected static const _cowWithoutLegs :Class;

    [Embed(source="alpha-back-leg.png")]
    protected static const _cowBackLeg :Class;

    [Embed(source="alpha-front-leg.png")]
    protected static const _cowFrontLeg :Class;

}
}
