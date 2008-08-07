//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.ScrollPolicy;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.data.all.MediaDesc;

public class FriendRenderer extends HBox 
{
    // Initialized by the ClassFactory
    public var mctx :MsoyContext;

    public function FriendRenderer () 
    {
        super();

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

        // The style name for this renderer isn't getting respected, and I'm through with trying
        // to get it to work, so lets just inline the styles here
        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 3);
        setStyle("paddingRight", 3);
        setStyle("verticalAlign", "middle");
        setStyle("horizontalGap", 0);

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

        if (this.data == null) {
            return;
        }

        var friend :FriendEntry = this.data as FriendEntry;
        _name = friend.name;

        var labelBox :VBox = new VBox();
        labelBox.verticalScrollPolicy = ScrollPolicy.OFF;
        labelBox.horizontalScrollPolicy = ScrollPolicy.OFF;
        labelBox.setStyle("verticalGap", 0);
        labelBox.width = 
            FriendsListPanel.POPUP_WIDTH - MediaDesc.THUMBNAIL_WIDTH / 2 - ScrollBar.THICKNESS -
            4 /* list border * 2 */ - 6 /* padding */;
        addChild(labelBox);

        var friendLabel :Label = new Label();
        friendLabel.styleName = "friendLabel";
        friendLabel.text = _name.toString();
        friendLabel.percentWidth = 100;
        friendLabel.width = labelBox.width;
        labelBox.addChild(friendLabel);

        var statusContainer :HBox = new HBox();
        statusContainer.setStyle("paddingLeft", 3);
        labelBox.addChild(statusContainer);
        var statusLabel :Label = new Label();
        statusLabel.styleName = "friendStatusLabel";
        statusLabel.text = friend.status;
        statusLabel.percentWidth = 100;
        statusLabel.width = labelBox.width - 3;
        statusContainer.addChild(statusLabel);

        addChild(MediaWrapper.createView(friend.photo, MediaDesc.HALF_THUMBNAIL_SIZE));
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var menuItems :Array = [];
        mctx.getMsoyController().addFriendMenuItems(_name, menuItems);
        CommandMenu.createMenu(menuItems, mctx.getTopPanel()).popUpAtMouse();
    }

    protected var _name :MemberName;
}
}
