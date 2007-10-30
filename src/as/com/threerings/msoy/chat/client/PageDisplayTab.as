//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import mx.events.ResizeEvent;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

/**
 * Displays a tab page with arbitrary HTML source, in the same space as other chat tabs.
 */
public class PageDisplayTab extends ChatTab
{
    public var tabName :String;

    public function PageDisplayTab (ctx :WorldContext, tabName :String)
    {
        super(ctx);
        this.tabName = tabName;

        _controller = new PageDisplayController(ctx, this);
        _textbox = new TextWithStyle();
    }

    public function getController () :PageDisplayController
    {
        return _controller;
    }

    public function init () :void
    {
        addEventListener(ResizeEvent.RESIZE, checkSizes);
        _textbox.addEventListener(Event.CHANGE, abortChangeEvent);
        _controller.init();
    }

    public function shutdown () :void
    {
        _controller.shutdown();
        _textbox.removeEventListener(Event.CHANGE, abortChangeEvent);
        removeEventListener(ResizeEvent.RESIZE, checkSizes);
    }

    public function displayHTML (source :String) :void
    {
        // perhaps validate here?
        _textbox.htmlText = source;
    }

    public function setStyleSheet (cssText :String) :void
    {
        _textbox.setStyleSheet(cssText);
    }

    // from ChatTab
    override public function sendChat (message :String) :void
    {
        // do nothing - help pages don't have chat channels hooked up to them
    }

    // from Container
    override protected function createChildren () :void
    {
        super.createChildren();

        _textbox.text = "";
        setBaseLayer(_textbox);
    }

    // from Container
    override protected function childrenCreated () :void
    {
        super.childrenCreated();
        
        checkSizes();
    }

    // EVENT HANDLERS
    
    /** Prevents the Event.CHANGE event from propagating up to the parent. */
    protected function abortChangeEvent (event :Event) :void {
        
        // This function is a horrible kluge around a Flex/Flexlib bug, where two events have
        // the same string id (flash.events.Event.CHANGE and mx.events.IndexChangedEvent.CHANGE).
        // This causes problems for this control's container (SuperTabNavigator), which tries
        // to listen for IndexChangedEvents, but ends up getting all Event instances as well,
        // and chokes during on the downcast at runtime.
        //
        // Until SuperTabNavigator is fixed, my solution is to intercept any Event.CHANGE events
        // from the text control. I'm pretty sure no parent container will miss them anyway. :)
        event.stopPropagation();
    }

    protected function checkSizes (... ignored) :void
    {
        // only try this if the container is ready
        if (stage != null) {
            var height :int = stage.stageHeight - ControlBar.HEIGHT - HeaderBar.HEIGHT -
                TopPanel.DECORATIVE_MARGIN_HEIGHT;
            
            // we don't have our proper width yet (the stage has not yet resized), so we have to
            // hardcode the width to the one we know we'll be receiving
            _textbox.width = TopPanel.RIGHT_SIDEBAR_WIDTH;
            _textbox.height = height;
            _textbox.move(0, 0);
        }
    }

    protected var _textbox :TextWithStyle;
    protected var _controller :PageDisplayController;
}
}


import flash.text.StyleSheet;
import mx.controls.Text;

/**
 * Flex controls in general don't support setting style sheets at runtime. But Text's internal
 * TextField instance does, so we just expose it. 
 */
internal class TextWithStyle extends Text
{
    public function setStyleSheet (cssDefinition :String) :void
    {
        var style :StyleSheet = new StyleSheet();
        style.parseCSS(cssDefinition);
        this.textField.styleSheet = style;
    }
}

