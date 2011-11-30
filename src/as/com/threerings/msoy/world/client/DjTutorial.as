//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Sprite;

import mx.core.UIComponent;

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

        var seq :TutorialSequenceBuilder = _ctx.getTutorialDirector().newSequence("djTutorial2")
            .showAlways();

        // TODO(bruno): Finalize text

        waitForPage(seq.newSuggestion("1. Click the shop tab"), Page.SHOP)
            .onShow(function () :void {
                arrowUp(-512, 0, function (address :Address) :Boolean { return address == null });
            })
            .queue();

        waitForPage(seq.newSuggestion("2. Go to the music section"),
                Page.SHOP, MsoyItemType.AUDIO.toByte())
            .onShow(function () :void {
                arrowRight(-0.01, 123, function (address :Address) :Boolean {
                    return address.page == Page.SHOP && address.args.length == 0 });
            })
            .queue();

        waitForPage(seq.newSuggestion("3. Pick an item"),
                Page.SHOP, "l", MsoyItemType.AUDIO.toByte())
            .queue();

        waitForCondition(seq.newSuggestion("4. Buy and add it to the room"),
            function () :Boolean {
                return !_ctx.getMemberObject().tracks.isEmpty();
            })
            .queue();

        waitForCondition(seq.newSuggestion("5. Close GWT"),
            function () :Boolean {
                return _ctx.getMsoyClient().getAddress() == null;
            })
            .onShow(clearArrows)
            .queue();

        step(seq.newSuggestion("6. You're done!"))
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
        return item.buttonCloses(true);
    }

    [Embed(source="../../../../../../../rsrc/media/arrows.swf", symbol="arrow_up")]
    protected static const ARROW_UP :Class;
    [Embed(source="../../../../../../../rsrc/media/arrows.swf", symbol="arrow_right")]
    protected static const ARROW_RIGHT :Class;

    protected var _ctx :WorldContext;

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
