//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Predicates;
import com.threerings.util.Util;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.Address;
import com.threerings.msoy.tutorial.client.TutorialDirector;

public class GeneralTips
{
    public function GeneralTips (ctx :WorldContext)
    {
        _ctx = ctx;
        var tut :TutorialDirector = ctx.getTutorialDirector();

        function notEditing () :Boolean {
            return !_ctx.getTopPanel().isEditingRoom();
        }

        tut.newTip("tipSecurity", xlate("i.link_security")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Account_security")).limit(notEditing)
            .queue();

        tut.newTip("tipCurrency", xlate("i.link_currency")).beginner().buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Currency")).limit(notEditing).queue();

        tut.newTip("tipSelling", xlate("i.link_selling")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("List")).intermediate()
            .limit(Predicates.createAnd(notEditing, _ctx.isRegistered)).queue();

        tut.newTip("tipWiki", xlate("i.link_wiki")).buttonCloses()
            .button(xlate("b.link_wiki"), viewWiki("")).limit(notEditing).queue();

        tut.newTip("tipStartupGuide", xlate("i.link_startup_guide")).beginner().buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Starting_out")).limit(notEditing).queue();

        // tut.newTip("tipGetBars", xlate("i.link_get_bars")).beginner().buttonCloses()
        //     .button(xlate("b.link_default"), viewWiki("Billing_FAQ")).limit(notEditing).queue();

        tut.newTip("tipShare", xlate("i.link_share")).buttonCloses()
            .button(xlate("b.link_default"), display(Address.SHARE)).limit(notEditing).queue();

        tut.newTip("tipInvite", xlate("i.link_invite")).buttonCloses()
            .button(xlate("b.link_invite"), display(Address.INVITE)).limit(notEditing).queue();

        tut.newTip("tipEmbed", xlate("i.link_embed")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Share_this_room")).limit(notEditing)
            .queue();

        // TODO: tipShareFB - share on facebook

        tut.newTip("tipTransactions", xlate("i.link_transactions")).beginner().buttonCloses()
            .button(xlate("b.link_default"), display(Address.TRANSACTIONS)).limit(notEditing)
            .queue();

        tut.newTip("tipTour", xlate("i.tour")).beginner().buttonCloses()
            .menuItemHighlight(_ctx.getControlBar().goBtn, WorldController.START_TOUR)
            .button(xlate("b.tour"), _ctx.getWorldController().handleStartTour).limit(notEditing)
            .queue();

        tut.newTip("tipGames", xlate("i.games")).beginner().buttonCloses()
            .button(xlate("b.games"), display(Address.GAMES)).limit(notEditing).queue();
    }

    protected function display (address :Address) :Function
    {
        return Util.adapt(_ctx.getWorldController().displayAddress, address);
    }

    protected function viewWiki (page :String) :Function
    {
        return display(Address.wiki(page));
    }

    protected static function xlate (msg :String) :String
    {
        return Msgs.NPC.get(msg);
    }

    protected var _ctx :WorldContext;
}
}
