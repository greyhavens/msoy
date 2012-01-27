//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Sprite;

import mx.core.UIComponent;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.display.DisplayUtil;
import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.data.Address;
import com.threerings.msoy.data.Page;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.tutorial.client.TutorialItemBuilder;
import com.threerings.msoy.tutorial.client.TutorialSequenceBuilder;

public class DjTutorial
{
    public function DjTutorial (ctx :WorldContext)
    {
        _ctx = ctx;

        var originalHome :int = ctx.getMemberObject().homeSceneId;
        _locObserver = new LocationAdapter(null, function (..._) :void {
            if (_ctx.getWorldController().getCurrentSceneId() != originalHome) {
                _ctx.getTutorialDirector().dismiss();
                _ctx.getLocationDirector().removeLocationObserver(_locObserver);
            }
        });
        _ctx.getLocationDirector().addLocationObserver(_locObserver);

        var seq :TutorialSequenceBuilder = _ctx.getTutorialDirector().newSequence("djTutorial")
            .showAlways();

        step(seq.newSuggestion("Hello, welcome to my night club! Ok, it's not much to look at... yet."))
            .button("Next", null)
            .queue();

        waitForPage(seq.newSuggestion("Help a tofu out, would you? I need someone with fine" +
            " musical taste to pick out a song over in the shop."), Page.SHOP)
            .onShow(function () :void {
                // Show the arrow while no tabs are open
                arrowUp(-512, 0, function (address :Address) :Boolean { return address == null });
            })
            .queue();

        waitForPage(seq.newSuggestion("The shop is stuffed with all sorts of useful goodies. Click" +
            " on the music section to look for songs."), Page.SHOP, MsoyItemType.AUDIO.toByte())
            .onShow(function () :void {
                arrowRight(-0.01, 123, function (address :Address) :Boolean {
                    return address.page == Page.SHOP && address.args.length == 0 });
            })
            .queue();

        waitForPage(seq.newSuggestion(
            "If you've got a song or genre in mind, you can search for it. Or just pick whatever" +
            " catches your eye. You can keep it afterwards!"),
                Page.SHOP, "l", MsoyItemType.AUDIO.toByte())
            .queue();

        waitForCondition(seq.newSuggestion("This one? I can't say it would have been my first" +
            " choice, but it's catchy... Once you've decided, buy it then add it to the room."),
            function () :Boolean {
                return !_ctx.getMemberObject().tracks.isEmpty();
            })
            .queue();

        waitForCondition(seq.newSuggestion("Not bad! That livens this place up a bit." +
            " You can press the X button on the very right to close the shop now."),
            function () :Boolean {
                return _ctx.getMsoyClient().getAddress() == null;
            })
            .onShow(clearArrows)
            .queue();

        // TODO(bruno): Explain the music widget?
        step(seq.newSuggestion("Maybe you can own your own club one day... try exploring," +
            " DJ-ing with other people and building your reputation. Off you go!"))
            .onShow(function () :void {
                // Tutorial complete, open the blast doors
                _ctx.getWorldDirector().completeDjTutorial();
            })
            .queue();

        seq.activate();
    }

    protected function arrow (x :Number, y :Number, showIf :Function, displayClass :Class) :void
    {
        if (_arrow == null) {
            var sprite :Sprite = new Sprite();
            sprite.graphics.beginFill(0xff0000);
            sprite.graphics.drawCircle(0, 0, 50);
            _arrow = new FlexWrapper(sprite);

            _ctx.getTopPanel().addChild(_arrow);
            _ctx.getClient().addEventListener(MsoyClient.GWT_PAGE_CHANGED, invalidateArrow);
        }

        _arrowStates.push(new ArrowState(x, y, showIf, displayClass));
        invalidateArrow();
    }

    protected function arrowUp (x :Number, y :Number, showIf :Function = null) :void
    {
        arrow(x, y, showIf, ARROW_UP);
    }

    protected function arrowRight (x :Number, y :Number, showIf :Function = null) :void
    {
        arrow(x, y, showIf, ARROW_RIGHT);
    }

    protected function invalidateArrow (_:*=null) :void
    {
        if (_arrow == null) {
            return;
        }

        var address :Address = _ctx.getMsoyClient().getAddress();
        var visible :Boolean = false;
        for each (var state :ArrowState in _arrowStates) {
            if (state.showIf == null || state.showIf(address)) {
                _arrow.setStyle(state.pos.x < 0 ? "right" : "left", Math.abs(state.pos.x));
                _arrow.setStyle(state.pos.y < 0 ? "bottom" : "top", Math.abs(state.pos.y));
                DisplayUtil.removeAllChildren(_arrow);
                _arrow.addChild(new state.displayClass());
                visible = true;
                break;
            }
        }
        _arrow.visible = visible;
    }

    protected function clearArrows () :void
    {
        if (_arrow == null) {
            return;
        }

        _arrowStates.length = 0;
        _ctx.getClient().removeEventListener(MsoyClient.GWT_PAGE_CHANGED, invalidateArrow);
        DisplayUtil.detach(_arrow);
        _arrow = null;
    }

    protected function waitForPage (
        item :TutorialItemBuilder, page :Page, ...args) :TutorialItemBuilder
    {
        return step(item)
            .limit(function () :Boolean {
                var address :Address = _ctx.getMsoyClient().getAddress();
                if (address == null && page == null) {
                    return false;
                }
                if (address != null && address.page == page) {
                    if (address.args.length < args.length) {
                        return true;
                    }
                    for (var ii :int = 0; ii < args.length; ++ii) {
                        if (address.args[ii] != args[ii]) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            });
    }

    protected function waitForCondition (
        item :TutorialItemBuilder, cond :Function) :TutorialItemBuilder
    {
        return step(item)
            .limit(function () :Boolean { return !cond() });
    }

    protected function step (item :TutorialItemBuilder) :TutorialItemBuilder
    {
        var action :String = item.getId();
        return item
            .onShow(function () :void {
                _ctx.getWorldController().trackEvent("tutorial", action);
            })
            .buttonCloses(true);
    }

    [Embed(source="../../../../../../../rsrc/media/arrows.swf", symbol="arrow_up")]
    protected static const ARROW_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/arrows.swf", symbol="arrow_right")]
    protected static const ARROW_RIGHT :Class;

    protected var _ctx :WorldContext;
    protected var _locObserver :LocationAdapter;

    protected var _arrow :UIComponent;
    protected var _arrowStates :Array = []; // of ArrowState
}
}

import flash.geom.Point;

class ArrowState
{
    public var pos :Point;
    public var showIf :Function;
    public var displayClass :Class;

    public function ArrowState (x :Number, y :Number, showIf :Function, displayClass :Class)
    {
        this.pos = new Point(x, y);
        this.showIf = showIf;
        this.displayClass = displayClass;
    }
}
