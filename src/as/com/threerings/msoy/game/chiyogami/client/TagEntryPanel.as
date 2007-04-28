package com.threerings.msoy.game.chiyogami.client {

import flash.events.Event;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.MsoyUI;

public class TagEntryPanel extends HBox
{
    public function TagEntryPanel ()
    {
        super();

        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // TODO: does chiyogami have translations?
        addChild(MsoyUI.createLabel("Enter tags:"));

        _text = new TextInput();
        _text.width = 100;
        _text.addEventListener(FlexEvent.ENTER, submitTags);
        _text.addEventListener(Event.CHANGE, handleTextChange);
        addChild(_text);

        _btn = new CommandButton();
        _btn.label = Msgs.GENERAL.get("b.ok");
        _btn.enabled = false;
        _btn.setFunction(submitTags);
        addChild(_btn);
    }

    protected function submitTags (... ignored) :void
    {
        _lastSubmitted = _text.text;
        CommandEvent.dispatch(this, ChiyogamiController.TAGS_ENTERED, _lastSubmitted);
        _btn.enabled = false;
    }

    protected function handleTextChange (... ignored) :void
    {
        _btn.enabled = (_text.text != _lastSubmitted);
    }

    protected var _text :TextInput;

    protected var _btn :CommandButton;

    protected var _lastSubmitted :String = "";
}
}
