//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import mx.containers.Canvas;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.flex.FlexWrapper;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;

import com.threerings.util.Command;
import com.threerings.util.CommandEvent;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

public class EmbedHeader extends Canvas
{
    public function EmbedHeader (ctx :WorldContext)
    {
        // this is the height of the header logo image.
        height = 50;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        _ctx = ctx;
        _ctx.getClient().addClientObserver(new ClientAdapter(null, didLogon));

        var logo :FlexWrapper = new FlexWrapper(new LOGO() as DisplayObject, true);
        logo.addEventListener(MouseEvent.CLICK, handleLogoClick);
        logo.setStyle("right", 0);
        logo.buttonMode = true;
        logo.mouseEnabled = true;
        addChild(logo);

        var title :Label = new Label();
        title.styleName = "embedHeaderLabel";
        title.text = Msgs.GENERAL.get("t.embed_header");
        title.setStyle("bottom", 0);
        // center between the right edge of the logo (126px wide) and the right edge of the canvas
        title.setStyle("horizontalCenter", 63);
        addChild(title);

        var signInObject :DisplayObject = new SIGN_IN() as DisplayObject;
        signInObject.x = signInObject.width / 2;
        signInObject.y = signInObject.height / 2 - 3;
        _signIn = new FlexWrapper(signInObject);
        _signIn.addEventListener(MouseEvent.CLICK, doLogon);
        _signIn.setStyle("right", 150); // TODO: we need a real HGroup, or something
        addChild(_signIn);

        var joinNowObject :DisplayObject = new JOIN_NOW() as DisplayObject;
        joinNowObject.x = joinNowObject.width / 2;
        joinNowObject.y = joinNowObject.height / 2 - 3;
        _joinNow = new FlexWrapper(joinNowObject);
        Command.bind(_joinNow, MouseEvent.CLICK, WorldController.CREATE_ACCOUNT);
        _joinNow.setStyle("right", 240); // TODO: we need a real HGroup, or something
        addChild(_joinNow);
    }

    protected function doLogon (event :MouseEvent) :void
    {
        (new LogonPanel(_ctx)).open();
    }

    protected function didLogon (event :ClientEvent) :void
    {
        if (_ctx.getMemberObject().isGuest()) {
            if (!contains(_signIn)) {
                addChild(_signIn);
                addChild(_joinNow);
            }
        } else {
            if (contains(_signIn)) {
                removeChild(_signIn);
                removeChild(_joinNow);
            }
        }
    }

    protected function handleLogoClick (... ignored) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        if (scene != null) {
            CommandEvent.dispatch(this, WorldController.VIEW_FULL_VERSION, scene.getId());
        } else {
            CommandEvent.dispatch(this, WorldController.VIEW_GAME,
                _ctx.getGameDirector().getGameId())
        }
        // used to be: _ctx.getWorldController().handleViewFullVersion(sceneId);
    }

    [Embed(source="../../../../../../pages/images/header/header_logo.png")]
    protected static const LOGO :Class;
    [Embed(source="../../../../../../rsrc/media/embedbuttons.swf#JoinNow")]
    protected static const JOIN_NOW :Class;
    [Embed(source="../../../../../../rsrc/media/embedbuttons.swf#SignIn")]
    protected static const SIGN_IN :Class;

    protected var _ctx :WorldContext;
    protected var _signIn :FlexWrapper;
    protected var _joinNow :FlexWrapper;
}
}
