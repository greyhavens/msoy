//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.flex.FlexWrapper;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;

import com.threerings.util.Command;
import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

public class EmbedHeader extends Canvas
{
    public static const HEIGHT :int = 30;

    public function EmbedHeader (ctx :WorldContext)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(new ClientAdapter(null, didLogon));

        // this is the height of the header logo image.
        height = HEIGHT;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        var box :HBox = new HBox();
        box.horizontalScrollPolicy = ScrollPolicy.OFF;
        box.setStyle("horizontalAlign", "right");
        box.setStyle("verticalAlign", "top");
        box.setStyle("horizontalGap", 4);
        box.setStyle("right", 0);
        box.setStyle("top", 0);
        addChild(box);

        box.addChild(new FlexWrapper(new FRILLY() as DisplayObject, true));

        var allgames :CommandButton = new CommandButton(null, MsoyController.VIEW_GAMES);
        allgames.styleName = "embedHeaderAllgamesButton";
        box.addChild(allgames);

        _signup = new CommandButton(null, WorldController.CREATE_ACCOUNT);
        _signup.styleName = "embedHeaderSignupButton";
        box.addChild(_signup);

        _logon = new CommandButton(null, doLogon);
        _logon.styleName = "embedHeaderLogonButton";
        box.addChild(_logon);

        FlexUtil.setVisible(_signup, false);
        FlexUtil.setVisible(_logon, false);

        var logo :FlexWrapper = new FlexWrapper(new LOGO() as DisplayObject, true);
        Command.bind(logo, MouseEvent.CLICK, MsoyController.VIEW_FULL_VERSION);
        logo.buttonMode = true;
        logo.mouseEnabled = true;
        box.addChild(logo);
    }

    /**
     * Set the name and logo for the current place. Either one may be null.
     */
    public function setPlaceName (name :String, logo :MediaDesc = null) :void
    {
        // out with the old
        if (_placeLogo != null) {
            removeChild(_placeLogo);
            _placeLogo = null;
        }
        if (_placeLabel != null) {
            removeChild(_placeLabel);
            _placeLabel  = null;
        }

        // in with the new
        var xx :int = 0;
        if (logo != null) {
            _placeLogo = MediaWrapper.createView(logo, MediaDesc.HALF_THUMBNAIL_SIZE);
            _placeLogo.x = xx;
            xx += PAD + _placeLogo.measuredWidth;
            addChild(_placeLogo);
        }
        if (name != null) {
            _placeLabel = FlexUtil.createLabel(name, "embedHeaderPlaceName");
            _placeLabel.x = xx;
            addChild(_placeLabel);
        }
    }

    protected function doLogon () :void
    {
        (new LogonPanel(_ctx)).open();
    }

    protected function didLogon (event :ClientEvent) :void
    {
        const isGuest :Boolean = _ctx.getMemberObject().isGuest();
        FlexUtil.setVisible(_logon, isGuest);
        FlexUtil.setVisible(_signup, isGuest);
    }

    [Embed(source="../../../../../../rsrc/media/skins/embedheader/logo.jpg")]
    protected static const LOGO :Class;
    [Embed(source="../../../../../../rsrc/media/skins/embedheader/frilly.jpg")]
    protected static const FRILLY :Class;

    protected static const PAD :int = 10;

    protected var _ctx :WorldContext;

    protected var _logon :CommandButton;
    protected var _signup :CommandButton;

    protected var _placeLogo :MediaWrapper;
    protected var _placeLabel :Label;
}
}
