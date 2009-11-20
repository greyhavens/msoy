//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Util;

import com.threerings.msoy.data.Address;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.tutorial.client.TutorialDirector;

public class GeneralTips
{
    public function GeneralTips (ctx :WorldContext)
    {
        _ctx = ctx;
        var tut :TutorialDirector = ctx.getTutorialDirector();

        tut.newTip("tipSecurity", xlate("i.link_security")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Account_security")).queue();

        tut.newTip("tipCurrency", xlate("i.link_currency")).beginner().buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Currency")).queue();

        tut.newTip("tipSelling", xlate("i.link_selling")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("List")).intermediate().queue();

        tut.newTip("tipWiki", xlate("i.link_wiki")).buttonCloses()
            .button(xlate("b.link_wiki"), viewWiki("")).queue();

        tut.newTip("tipStartupGuide", xlate("i.link_startup_guide")).beginner().buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Starting_out")).queue();

        tut.newTip("tipGetBars", xlate("i.link_get_bars")).beginner().buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Billing_FAQ")).queue();

        tut.newTip("tipShare", xlate("i.link_share")).buttonCloses()
            .button(xlate("b.link_default"), display(Address.SHARE)).queue();

        tut.newTip("tipInvite", xlate("i.link_invite")).buttonCloses()
            .button(xlate("b.link_invite"), display(Address.INVITE)).queue();

        tut.newTip("tipEmbed", xlate("i.link_embed")).buttonCloses()
            .button(xlate("b.link_default"), viewWiki("Share_this_room")).queue();

        tut.newTip("tipClub", xlate("i.link_club")).buttonCloses()
            .button(xlate("b.link_club"), display(Address.SUBSCRIBE)).queue();

        // TODO: tipShareFB - share on facebook

        tut.newTip("tipTransactions", xlate("i.link_transactions")).beginner().buttonCloses()
            .button(xlate("b.link_default"), display(Address.TRANSACTIONS)).queue();
    }

    protected function display (address :Address) :Function
    {
        return Util.adapt(_ctx.getWorldController().displayAddress, address);
    }

    protected function viewWiki (page :String) :Function
    {
        return Util.adapt(_ctx.getMsoyController().handleViewUrl, WIKI + page);
    }

    protected static function xlate (msg :String) :String
    {
        return Msgs.NPC.get(msg);
    }

    protected var _ctx :WorldContext;

    protected static const WIKI :String = "http://wiki.whirled.com/";
}
}
