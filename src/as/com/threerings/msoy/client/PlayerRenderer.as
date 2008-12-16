//
// $Id$

package com.threerings.msoy.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.Text;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.ScrollPolicy;

import com.threerings.util.CommandEvent;
import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.PlayerEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

public class PlayerRenderer extends HBox 
{
    // Initialized by the ClassFactory
    public var mctx :MsoyContext;

    public function PlayerRenderer () 
    {
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
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
        // TODO: This should work, try later
        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 3);
        setStyle("paddingRight", 3);
        setStyle("verticalAlign", "middle");
        setStyle("horizontalGap", 0);

        configureUI();
    }

    /**
     * Set up custom content to show for this renderer besides just the profile photo.
     */
    protected function addCustomControls (content :VBox) :void
    {
        // nada
    }

    /**
     * Update the UI elements with the data we're displaying.
     */
    protected function configureUI () :void
    {
        removeAllChildren();

        var player :PlayerEntry = this.data as PlayerEntry;

        if (player != null) {
            var icon :MediaWrapper = MediaWrapper.createView(player.name.getPhoto(), getIconSize());
            addChild(icon);
            var content :VBox = new VBox();
            content.verticalScrollPolicy = ScrollPolicy.OFF;
            content.horizontalScrollPolicy = ScrollPolicy.OFF;
            content.setStyle("verticalGap", 0);
            content.width = parent.width - icon.measuredWidth;
            addChild(content);

            addCustomControls(content);
        }
    }

    /**
     * Get the size of the icon to use for this widget.
     */
    protected function getIconSize () :int
    {
        return MediaDesc.HALF_THUMBNAIL_SIZE;
    }
}
}
