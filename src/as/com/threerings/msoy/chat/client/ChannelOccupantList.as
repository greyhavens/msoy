//
// $Id$

package com.threerings.msoy.chat.client {

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.VScrollBar;

import mx.core.ScrollPolicy;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.MemberName;

public class ChannelOccupantList extends HBox
{
    public function ChannelOccupantList () 
    {
        super();

        // set up the UI
        width = 316;
        height = 125;

        styleName = "channelOccupantList";

        // we have to take care of this ourselves with a scroll bar on the left, as you can't move 
        // the horizontal position of a vertical scroll bar in a flex component.
        verticalScrollPolicy = ScrollPolicy.OFF;

        addChild(_scroll = new VScrollBar());
        _scroll.percentHeight = 100;

        addChild(_playersContainer = new ListBox(_scroll));
    }

    public function clear () :void 
    {
        _playersContainer.removeAllChildren();
    }

    public function addChatter (info :VizMemberName) :void
    {
        var ii :int;
        for (ii = 0; ii < _playersContainer.numChildren; ii++) {
            var current :VizMemberName = 
                (_playersContainer.getChildAt(ii) as ChatterRenderer).data as VizMemberName;
            if (MemberName.BY_DISPLAY_NAME(current, info) > 0) {
                break;
            }
        }

        if (ii != 0) {
            var previous :VizMemberName = 
                (_playersContainer.getChildAt(ii - 1) as ChatterRenderer).data as VizMemberName;
            if (info.equals(previous)) {
                // we were told to add someone that's already in the list
                return;
            }
        }

        var renderer :ChatterRenderer = new ChatterRenderer();
        renderer.data = info;
        _playersContainer.addChildAt(renderer, ii);
        measure();
    }

    public function removeChatter (name :Name) :void
    {
        for (var ii :int = 0; ii < _playersContainer.numChildren; ii++) {
            var current :VizMemberName = 
                (_playersContainer.getChildAt(ii) as ChatterRenderer).data as VizMemberName;
            if (current.equals(name)) {
                _playersContainer.removeChildAt(ii);
                break;
            }
        }
    }

    public function setRightSideScrollbar (rightSide :Boolean) :void
    {
        if ((getChildIndex(_scroll) == 0) != rightSide) {
            return; // no change
        }

        removeChild(_scroll);
        if (rightSide) {
            addChild(_scroll);
        } else {
            addChildAt(_scroll, 0);
        }
    }

    private static const log :Log = Log.getLog(ChannelOccupantList);

    protected var _scroll :VScrollBar;
    protected var _playersContainer :ListBox;
}
}

import flash.display.DisplayObject;

import flash.geom.Rectangle;

import flash.text.TextFieldAutoSize;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Image;
import mx.controls.Label;
import mx.controls.VScrollBar;

import mx.events.ScrollEvent;

import com.threerings.flex.FlexWrapper;

import com.threerings.util.Log;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.world.client.NameField;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.item.data.all.MediaDesc;

class ListBox extends VBox 
{
    public function ListBox (scrollBar :VScrollBar)
    {
        _scrollBar = scrollBar;
        _scrollBar.addEventListener(ScrollEvent.SCROLL, onScroll);

        percentWidth = 100;
        percentHeight = 100;
        setStyle("borderStyle", "none");
    }

    override protected function updateDisplayList (unscaledWidth :Number,
                                                   unscaledHeight :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        if (numChildren > 0) {
            var dispObj :DisplayObject = getChildAt(numChildren - 1);
            var bottom :int = dispObj.y + dispObj.height;
            _scrollBar.setScrollProperties(parent.height, parent.height, bottom);
        }
    }

    protected function onScroll (event :ScrollEvent) :void
    {
        scrollRect = new Rectangle(0, event.position - parent.height, parent.width, parent.height);
    }

    private static const log :Log = Log.getLog(ListBox);
    
    protected var _scrollBar :VScrollBar;
}

// ChannelOccupantList doesn't currently use a List, but in case it does in the future, this should
// drop in as an item renderer for VizMemberNames
class ChatterRenderer extends HBox
{
    public function ChatterRenderer () 
    {
        super();

        styleName = "chatterRenderer";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        // the horizontalGap should be 8...
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        if (processedDescriptors) {
            configureUI();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren(); 

        _headshot = MediaWrapper.createView(null, MediaDesc.QUARTER_THUMBNAIL_SIZE);
        addChild(_headshot);

        addChild(new FlexWrapper(_nameField = new NameField()));
        _nameField.autoSize = TextFieldAutoSize.LEFT;

        configureUI();
    }

    /**
     * Update the UI elements with the data we're displaying.
     */
    protected function configureUI () :void
    {
        var chatter :VizMemberName = this.data as VizMemberName;
        if (chatter != null) {
            _nameField.text = chatter.toString();
            callLater(function () :void {
                // setting y = -_nameField.textHeight / 2 immediately doesn't seem to properly 
                // account for the boldness/glowfilter
                _nameField.y = -_nameField.height / 2;
            });
            _headshot.setMediaDesc(chatter.getPhoto());

        } else {
            _nameField.text = "";
            _headshot.shutdown();
        }
    }

//    private static const log :Log = Log.getLog(ChatterRenderer);

    protected var _headshot :MediaWrapper;
    protected var _nameField :NameField;
}
