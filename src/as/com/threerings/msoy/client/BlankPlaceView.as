//
// $Id$

package com.threerings.msoy.client {

import mx.containers.Canvas;

import com.threerings.util.Log;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.data.MemberClientObject;
import com.threerings.msoy.ui.LoadingSpinner;

/**
 * Displays a blank view when we have nothing else to display. Can optionally display our loading
 * graphics with a status message.
 */
public class BlankPlaceView extends Canvas
    implements PlaceView, AttributeChangeListener
{
    public function BlankPlaceView (ctx :MsoyContext)
    {
        // we do some hackery here to obtain our width and height because we want to precisely
        // match the Preloader math which uses the full stage width and height, but our math is
        // going to be sullied by the header bar, embed bar and control bar *and* we are created
        // during TopPanel's constructor, so we can't ask it how big it is
        var swidth :int = UberClient.getApplication().stage.stageWidth;
        var sheight :int = UberClient.getApplication().stage.stageHeight;
        var hheight :int = HeaderBar.getHeight(ctx.getMsoyClient());
//        if (ctx.getMsoyClient().isEmbedded()) {
//            hheight += EmbedHeader.HEIGHT;
//        }

        _spinner = new LoadingSpinner();
        addChild(new FlexWrapper(_spinner));
        _spinner.x = (swidth - LoadingSpinner.WIDTH) / 2;
        _spinner.y = (sheight - LoadingSpinner.HEIGHT) / 2 - hheight;

        // if we're upselling, we want to preserve continuity with the preloader splash
        if (ctx.getMsoyClient().getEmbedding().shouldUpsellWhirled()) {
            _spinner.setStatus(""); // use a blank status, we have our splash text already
            // TODO: could use createSharableLink here except we're not logged on
            var msg :String = Msgs.GENERAL.get("m.embed_splash", DeploymentConfig.serverURL);
            addChild(new FlexWrapper(Preloader.makeSplashText(msg, swidth, _spinner.y)));
        } else {
            _spinner.setStatus(Msgs.GENERAL.get("m.ls_connecting"));
        }
    }

    /**
     * When we successfully log into the Whirled server, we will be given a MemberClientObject
     * in preparation for the loading of the full MemberObject. At this point we can start
     * showing progress information.
     */
    public function gotClientObject (clobj :MemberClientObject) :void
    {
        Log.getLog(this).info("gotClientObject()", "position", clobj.position);
        // only bother if there's at least 3 people in line before us
        if (clobj.position > 2) {
            clobj.addListener(this);
        }
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (MemberClientObject.POSITION == event.getName()) {
            _spinner.setStatus(Msgs.GENERAL.get("m.ls_queue", event.getValue()));
        }
    }

    // from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nada
    }

    // from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nada
    }

    protected var _spinner :LoadingSpinner;
}
}
