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
        width = 300;
        height = 125;

        styleName = "channelOccupantList";

        // we have to take care of this ourselves with a scroll bar on the left, as you can't move 
        // the horizontal position of a vertical scroll bar in a flex component.
        verticalScrollPolicy = ScrollPolicy.OFF;

        addChild(_scroll = new VScrollBar());
        _scroll.percentHeight = 100;

        addChild(_playersContainer = new ListBox(_scroll));
    }

    public function addChatter (info :VizMemberName) :void
    {
        for (var ii :int = 0; ii < _playersContainer.numChildren; ii++) {
            var current :VizMemberName = 
                (_playersContainer.getChildAt(ii) as ChatterRenderer).data as VizMemberName;
            if (MemberName.BY_DISPLAY_NAME(current, info) > 0) {
                break;
            }
        }
        // do it outside to catch the case where this chatter goes at the end, or the list is 
        // empty
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

    private static const log :Log = Log.getLog(ChannelOccupantList);

    protected var _scroll :VScrollBar;
    protected var _playersContainer :ListBox;
}
}

import flash.display.DisplayObject;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Image;
import mx.controls.Label;
import mx.controls.VScrollBar;

import com.threerings.util.Log;

import com.threerings.msoy.data.VizMemberName;

class ListBox extends VBox 
{
    public function ListBox (scrollBar :VScrollBar)
    {
        _scrollBar = scrollBar;

        percentWidth = 100;
        percentHeight = 100;
        setStyle("borderStyle", "none");
    }

    override protected function updateDisplayList (unscaledWidth :Number,
                                                   unscaledHeight :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        // TODO: if the bottom of the last entry is lower than parent.heigh, then we need to 
        // mess with the scroll bar
        var dispObj :DisplayObject = getChildAt(numChildren - 1);
        var bottom :int = dispObj.y + dispObj.height;
        log.debug("bottom [" + bottom + "]");
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

        addChild(_headshot = new Image());
        _headshot.height = 20; // 1/3 of headshot size
        _headshot.width = 20;
        _headshot.maintainAspectRatio = true;

        addChild(_nameLabel = new Label());
        _nameLabel.width = 208;

        configureUI();
    }

    /**
     * Update the UI elements with the data we're displaying.
     */
    protected function configureUI () :void
    {
        var chatter :VizMemberName = this.data as VizMemberName;
        if (chatter != null) {
            _nameLabel.text = chatter.toString();
            _headshot.source = chatter.getPhoto().getMediaPath();

        } else {
            _nameLabel.text = "";
            _headshot.source = null;
        }
    }
    
    private static const log :Log = Log.getLog(ChatterRenderer);

    protected var _headshot :Image;
    protected var _nameLabel :Label;
}
