//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

public class FriendRenderer extends HBox 
{
    public function FriendRenderer () 
    {
        super();

        styleName = "friendRenderer";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        addEventListener(MouseEvent.CLICK, handleClick);
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

    /**
     * Update the UI elements with the data we're displaying.
     */
    protected function configureUI () :void
    {
        // TODO: This renderer represents a considerable departure from the way that we display
        // names in msoy.  It is quite a bit different from how we show them in channel and game
        // occupant lists, and floating over avatars.  I brought this up to bill, and he says we 
        // need to just go in and change how they look everywhere.  This style won't look good
        // everywhere, so I'm waiting to see if we come up with a single style that looks good
        // everywhere before just using MsoyNameLabelCreator here, which is what this should 
        // probably be doing (or the whole list should just be a new kind of PlayerList).

        removeAllChildren();

        if (this.data == null || !(this.data is Array) || (this.data as Array).length != 2) {
            return;
        }

        _mctx = this.data[0] as MsoyContext;
        var friend :FriendEntry = this.data[1] as FriendEntry;
        _name = friend.name;

        var friendLabel :Label = new Label();
        friendLabel.styleName = "friendLabel";
        friendLabel.text = _name.toString();
        addChild(friendLabel);

        var spacer :HBox = new HBox();
        spacer.percentWidth = 100;
        addChild(spacer);

        addChild(MediaWrapper.createView(friend.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE));
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var menuItems :Array = [];
        _mctx.getMsoyController().addMemberMenuItems(_name, menuItems);
        CommandMenu.createMenu(menuItems, _mctx.getTopPanel()).popUpAtMouse();
    }

    protected var _mctx :MsoyContext;
    protected var _name :MemberName;
}
}
