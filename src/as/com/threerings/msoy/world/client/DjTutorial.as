//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.data.Address;
import com.threerings.msoy.data.Page;
import com.threerings.msoy.item.data.all.MsoyItemType;
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
            .queue();

        waitForPage(seq.newSuggestion("2. Go to the music section"),
                Page.SHOP, MsoyItemType.AUDIO.toByte())
            .queue();

        waitForPage(seq.newSuggestion("3. Pick an item"),
                Page.SHOP, "l", MsoyItemType.AUDIO.toByte())
            .queue();

        waitForCondition(seq.newSuggestion("4. Buy and add it to the room"),
            function () :Boolean {
                return !_ctx.getMemberObject().tracks.isEmpty()
            })
            .queue();

        waitForCondition(seq.newSuggestion("5. Close GWT"),
            function () :Boolean {
                return _ctx.getMsoyClient().getAddress() == null;
            })
            .queue();

        seq.activate();
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

    protected var _ctx :WorldContext;
}
}
