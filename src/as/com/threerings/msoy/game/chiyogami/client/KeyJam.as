package com.threerings.msoy.game.chiyogami.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.KeyboardEvent;

import flash.text.TextField;

import flash.ui.Keyboard;

import com.threerings.flash.ClearingTextField;

[SWF(width="450", height="100")]
public class KeyJam extends Sprite
{
    public function KeyJam (millisPerBeat :Number = 1000)
    {
        var bkg :DisplayObject = new BOOMBOX() as DisplayObject;
        bkg.y = (100 - bkg.height) / 2;
        bkg.x = bkg.width / 2;
        addChild(bkg);

        // Question: why does Sprite claim to generate key events when
        // I have never seen it capable of doing so? Flash blows.
        var keyGrabber :TextField = new TextField();
        keyGrabber.selectable = false;
        keyGrabber.width = 400;
        keyGrabber.height = 200;
        addChild(keyGrabber);

        _label = new ClearingTextField();
        _label.background = true;
        _label.selectable = false;
        _label.width = 400;
        _label.setText("Welcome to KeyJam", 5);
        _label.height = _label.textHeight + 4; // flash blows: really
        _label.y = 200 - _label.height;
        addChild(_label);

        _timingBar = new TimingBar(500, 20, millisPerBeat);
        _timingBar.y = 100;
        addChild(_timingBar);

        addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown);

        setNewSequence();
    }

    protected function setNewSequence () :void
    {
        var keySprite :KeySprite;

        // clear any existing keys
        for each (keySprite in _keySprites) {
            removeChild(keySprite);
        }
        _keySprites.length = 0; // truncate

        // generate a new sequence
        var seq :Array = generateKeySequence(
            Math.min(MAX_SEQUENCE_LENGTH, _level + 3));

        for (var ii :int = 0; ii < seq.length; ii++) {
            var key :int = int(seq[ii]);
            keySprite = new KeySprite(key, classForKey(key));
            _keySprites.push(keySprite);
            keySprite.x = ii * (KeySprite.WIDTH);
            addChild(keySprite);
        }
        _seqIndex = 0;
    }

    protected function classForKey (key :int) :Class
    {
        switch (key) {
        default:
        case Keyboard.UP:
            return UP_ARROW;

        case Keyboard.DOWN:
            return DOWN_ARROW;

        case Keyboard.LEFT:
            return LEFT_ARROW;

        case Keyboard.RIGHT:
            return RIGHT_ARROW;
        }
    }


    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        var code :int = event.keyCode;
        if (_seqIndex < _keySprites.length) {
            // see if the key pressed is the correct next one in the sequence
            var keySprite :KeySprite = (_keySprites[_seqIndex] as KeySprite);
            if (keySprite.getKey() == code) {
                // yay
                keySprite.setHit(true);
                _seqIndex++;

            } else {
                // uh-ok, they booched it!
                resetSequenceProgress();
            }

        } else {
            if (code == Keyboard.SPACE) {
                // the sequence was generated!
                finishLevel();

            } else {
                // total booch!
                resetSequenceProgress();
            }
        }

        // macrodobe can chew my sack for not making this the default
        event.updateAfterEvent(); // flash blows
    }

    protected function resetSequenceProgress () :void
    {
        while (_seqIndex > 0) {
            _seqIndex--;
            var keySprite :KeySprite = (_keySprites[_seqIndex] as KeySprite);
            keySprite.setHit(false);
        }

        _booches++;
        _label.setText("Oh, ye booched it!", 5);
    }

    /**
     * Generate a new key sequence.
     */
    protected function generateKeySequence (length :int = 3) :Array
    {
        var seq :Array = [];
        while (length-- > 0) {
            seq.push(ARROW_KEYS[int(Math.random() * ARROW_KEYS.length)]);
        }
        return seq;
    }

    /**
     * The user hit the space bar after duping the sequence, let's
     * see how they did.
     */
    protected function finishLevel () :void
    {
        var result :Number = _timingBar.checkNeedle();
        trace("Result: " + result);

        var feedback :String;
        if (result == 1) {
            feedback = "PERFECT!";

        } else if (result >= .98) {
            feedback = "Outstanding!";

        } else if (result >= .95) {
            feedback = "Great!";

        } else if (result >= .9) {
            feedback = "Good";

        } else if (result >= .8) {
            feedback = "Nice";

        } else if (result >= .5) {
            feedback = "okay";

        } else if (result >= .1) {
            feedback = "poor";

        } else {
            feedback = "piss-poor";
        }

        // issue feedback, fade out the timer
        _label.setText(feedback, 5);

        // TODO: Scoring

        // move to the next level
        _booches = 0;
        _level++;
        setNewSequence();
    }

    protected var _keySprites :Array = [];

    /** The position in the sequence we're waiting for. */
    protected var _seqIndex :int = 0;

    /** Which level is the user on? */
    protected var _level :int = 0;

    /** The number of booches on the current level. */
    protected var _booches :int = 0;

    /** A label where we give encouragement and ridicule to yon user. */
    protected var _label :ClearingTextField;

    /** The timing bar. */
    protected var _timingBar :TimingBar;

    protected static const PAD :int = 10;

    protected static const ARROW_KEYS :Array = [
        Keyboard.UP, Keyboard.DOWN, Keyboard.LEFT, Keyboard.RIGHT ];

    protected static const MAX_SEQUENCE_LENGTH :int = 7;

    [Embed(source="keyjam.swf#boombox")]
    protected static const BOOMBOX :Class;

    [Embed(source="keyjam.swf#up")]
    protected static const UP_ARROW :Class;

    [Embed(source="keyjam.swf#down")]
    protected static const DOWN_ARROW :Class;

    [Embed(source="keyjam.swf#left")]
    protected static const LEFT_ARROW :Class;

    [Embed(source="keyjam.swf#right")]
    protected static const RIGHT_ARROW :Class;
}
}
