//
// $Id$

package com.threerings.msoy.chat.client {

import com.whirled.ui.NameLabelCreator;
import com.whirled.ui.PlayerList;

public class ChannelPlayerList extends PlayerList
{
    public function ChannelPlayerList (labelCreator :NameLabelCreator = null)
    {
        super(labelCreator);   
    }

    override protected function getRenderingClass () :Class
    {
        return ChannelPlayerRenderer;
    }
}
}

import flash.display.DisplayObject;

import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.whirled.ui.NameLabel;
import com.whirled.ui.NameLabelCreator;

import com.threerings.msoy.data.VizMemberName;

class ChannelPlayerRenderer extends HBox
{
    public function ChannelPlayerRenderer ()
    {
        super();

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        setStyle("backgroundAlpha", 0);
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

        configureUI();
    }

    protected function configureUI () :void
    {
        if (this.data != null && (this.data is Array) && (this.data as Array).length == 2) {
            var dataArray :Array = this.data as Array;
            var creator :NameLabelCreator = dataArray[0] as NameLabelCreator;
            var name :VizMemberName = dataArray[1] as VizMemberName;
            if (_currentName == null || !_currentName.equals(name) || 
                _currentName.toString() != name.toString() || 
                !_currentName.getPhoto().equals(name.getPhoto())) {
                if (_currentLabel != null && contains(_currentLabel as DisplayObject)) {
                    removeChild(_currentLabel as DisplayObject);
                }
                addChild((_currentLabel = creator.createLabel(name)) as DisplayObject);
                _currentLabel.percentWidth = 100;
                _currentName = name;
            }
        } else {
            if (_currentLabel != null && contains(_currentLabel as DisplayObject)) {
                removeChild(_currentLabel as DisplayObject);
            }
            _currentLabel = null;
            _currentName = null;
        }
    }

    protected var _currentLabel :NameLabel;
    protected var _currentName :VizMemberName;
}
