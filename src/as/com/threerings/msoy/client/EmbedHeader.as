//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;

import flash.net.navigateToURL; // function import
import flash.net.URLRequest;

import mx.containers.Canvas;

import mx.controls.Label;

import com.threerings.flex.FlexWrapper;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.world.client.WorldContext;

public class EmbedHeader extends Canvas
{
    public function EmbedHeader (ctx :WorldContext)
    {
        // this is the height of the header logo image.
        height = 50;

        _ctx = ctx;
        _ctx.getClient().addClientObserver(new ClientAdapter(null, didLogon));

        var logo :FlexWrapper = new FlexWrapper(new LOGO() as DisplayObject);
        logo.addEventListener(MouseEvent.CLICK, function (...ignored) :void {
            navigateToURL(new URLRequest(DeploymentConfig.serverURL), "_top");
        });
        logo.setStyle("left", 0);
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
        signInObject.x = -signInObject.width / 2;
        signInObject.y = signInObject.height / 2 - 3;
        _signIn = new FlexWrapper(signInObject);
        _signIn.addEventListener(MouseEvent.CLICK, function (...ignored) :void {
            (new LogonPanel(ctx)).open();
        });
        _signIn.setStyle("right", 10);
        addChild(_signIn);

        var joinNowObject :DisplayObject = new JOIN_NOW() as DisplayObject;
        joinNowObject.x = -joinNowObject.width / 2;
        joinNowObject.y = joinNowObject.height / 2 - 3;
        _joinNow = new FlexWrapper(joinNowObject);
        _joinNow.addEventListener(MouseEvent.CLICK, function (...ignored) :void {
            navigateToURL(new URLRequest(DeploymentConfig.serverURL + "#account-create"), "_top");
        });
        _joinNow.setStyle("right", 100);
        addChild(_joinNow);
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
