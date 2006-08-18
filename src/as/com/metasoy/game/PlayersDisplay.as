package com.metasoy.game {

import flash.display.Sprite;

import flash.text.StyleSheet;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;

/**
 * A sample component that displays the players of a game.
 * If the game has a turn holder, the current turn holder will be highlighted.
 *
 * This class demonstrates that the 'Game' interface may be implemented
 * by any DisplayObject that want access to the GameObject, not just the
 * actual DisplayObject that is displaying the game. Here, all we are
 * interested in is the names of the players and the current turn holder.
 *
 * You may use this, with any modifications you desire, in your game. Feel
 * free to copy/modify or extend this class.
 */
public class PlayersDisplay extends Sprite
    implements Game
{
    // implementation of Game method
    public function setGameObject (gameObj :GameObject) :void
    {
        if (_gameObj != null) {
            return;
        }

        _gameObj = gameObj;
        _gameObj.addEventListener(StateChangedEvent.TURN_CHANGED, turnChanged);

        setupPlayers();
    }

    /**
     * Set up the player labels and configure the look of the entire UI.
     */
    protected function setupPlayers () :void
    {
        var y :Number = BORDER;
        // create a label at the top, above the player names
        var label :TextField = new TextField();
// damn stylesheet doesn't seem to actually -work-
//        var style :StyleSheet = new StyleSheet();
//        style.fontWeight = "bold";
//        style.color = "#0000FF";
//        style.fontFamily = "serif";
//        style.fontSize = 18;
//        label.styleSheet = style;
        label.autoSize = TextFieldAutoSize.LEFT;
        label.selectable = false;
        label.text = "Players";
        label.x = BORDER;
        label.y = y;
        addChild(label);
        y += label.textHeight;

        var maxWidth :Number = label.textWidth;

        // create a label for each player
        for each (var name :String in _gameObj.getPlayerNames()) {
            y += PAD;
            label = new TextField();
            label.autoSize = TextFieldAutoSize.LEFT;
            label.background = true;
            label.selectable = false;
            label.text = name;
            label.x = BORDER;
            label.y = y;
            addChild(label);
            y += label.textHeight;
            maxWidth = Math.max(maxWidth, label.textWidth);

            _playerLabels.push(label);
        }

        // make all the player labels the same width
        // (looks nice when highlighted)
        for each (label in _playerLabels) {
            label.autoSize = TextFieldAutoSize.NONE;
            label.width = maxWidth;
        }

        // draw a blue rectangle around everything
        graphics.clear();
        graphics.lineStyle(1, 0x0000FF);
        graphics.drawRect(0, 0, maxWidth + (BORDER * 2), y + BORDER);

        displayCurrentTurn();
    }

    /**
     * Re-set the background color for every player label, highlighting
     * only the player who has the turn.
     */
    protected function displayCurrentTurn () :void
    {
        var idx :int = _gameObj.getTurnHolderIndex();
        for (var ii :int = 0; ii < _playerLabels.length; ii++) {
            var label :TextField = (_playerLabels[ii] as TextField);
            label.backgroundColor = (ii == idx) ? TURN_BACKGROUND
                                                : NORMAL_BACKGROUND;
        }
    }

    /**
     * Registered to receive TURN_CHANGED events from the GameObject.
     */
    protected function turnChanged (event :StateChangedEvent) :void
    {
        displayCurrentTurn();
    }

    /** Our game object. */
    protected var _gameObj :GameObject;

    /** An array of labels, one for each player name. */
    protected var _playerLabels :Array = [];

    /** The background color for players that doesn't have the turn. */
    protected static const NORMAL_BACKGROUND :uint = 0xFFFFFF;

    /** The background color for players that do have the turn. */
    protected static const TURN_BACKGROUND :uint = 0xFF9999;

    /** The number of pixels to border around all the names. */
    protected static const BORDER :int = 6;

    /** The additional padding inserted between names. */
    protected static const PAD :int = 2;
}
}
