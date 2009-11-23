//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.PageCommand;

import com.threerings.msoy.data.Address;
import com.threerings.msoy.data.Page;

import com.threerings.msoy.tutorial.client.TutorialSequenceBuilder;

/**
 * Various tutorial items shown when the me page is opened.
 */
public class MePageTutorial
{
    public function MePageTutorial (ctx :WorldContext) :void
    {
        _ctx = ctx;

        // most of the me page tutorial doesn't make sense if you're not registered
        if (_ctx.isRegistered()) {
            _ctx.getMsoyClient().addEventListener(MsoyClient.GWT_PAGE_CHANGED, onPageChange);
        }
    }

    protected function onPageChange (evt :ValueEvent) :void
    {
        _current = evt.value as Address;

        if (_trigger != null) {
            _trigger.activate(_current);
            _trigger = null;
            return;
        }

        if (!Address.ME.equals(_current)) {
            return;
        }

        var sequence :TutorialSequenceBuilder;
        sequence = _ctx.getTutorialDirector().newSequence("mePage").singles().newbie();

        // gwt intro
        sequence.newSuggestion(xlate("i.me_intro"))
            .button(xlate("b.me_intro"), null)
            .finishText(xlate("i.me_intro_finish")).queue();

        // name editing
        sequence.newSuggestion(xlate("i.me_name_change"))
            .button(xlate("b.me_name_change"), editProfile)
            .finishText(xlate("i.me_name_change_finish")).queue();

        // info editing
        sequence.newSuggestion(xlate("i.me_tell_us"))
            .button(xlate("b.me_tell_us"), editInfo)
            .finishText(xlate("i.me_tell_us_finish")).queue();

        // share
        sequence.newSuggestion(xlate("i.me_share"))
            .button(xlate("b.me_share"), Util.adapt(display, Address.SHARE))
            .buttonCloses().queue();

        // invite
        sequence.newSuggestion(xlate("i.me_invite"))
            .button(xlate("b.me_invite"), Util.adapt(display, Address.INVITE))
            .buttonCloses().queue();

        // contests
        sequence.newSuggestion(xlate("i.me_contests"))
            .button(xlate("b.me_contests"), Util.adapt(display, Address.CONTESTS))
            .buttonCloses().queue();

        // passport
        sequence.newSuggestion(xlate("i.me_stamps"))
            .button(xlate("b.me_stamps"), Util.adapt(display, Address.PASSPORT))
            .buttonCloses().queue();

        // if the tutorial doesn't activate for some reason, just disable for the remainder of the
        // session rather than always reattempting every time the me page is opened
        if (!sequence.activate()) {
            _ctx.getMsoyClient().removeEventListener(MsoyClient.GWT_PAGE_CHANGED, onPageChange);
        }
    }

    protected function editProfile () :void
    {
        invokeProfile(PageCommand.editProfile);
    }

    protected function editInfo () :void
    {
        invokeProfile(PageCommand.editInfo);
    }

    protected function invokeProfile (editFn :Function) :void
    {
        var profile :Address = Address.profile(getMemberId());
        if (!display(profile)) {
            return;
        }

        var trigger :Trigger = new Trigger(profile, Util.adapt(editFn, _ctx));
        if (!trigger.activate(_current)) {
            _trigger = trigger;
        }
    }

    protected function getMemberId () :int
    {
        return _ctx.getMemberObject().memberName.getMemberId();
    }

    protected function display (addr :Address) :Boolean
    {
        return _ctx.getWorldController().displayAddress(addr);
    }

    protected function xlate (msg :String) :String
    {
        return Msgs.NPC.get(msg);
    }

    protected var _ctx :WorldContext;
    protected var _current :Address;
    protected var _trigger :Trigger;
}
}

import com.threerings.msoy.data.Address;

import flash.utils.setTimeout; // function import

class Trigger
{
    public function Trigger (address :Address, action :Function)
    {
        _address = address;
        _action = action;
    }

    public function activate (address :Address) :Boolean
    {
        if (_address.equals(address)) {
            // call on the next frame
            setTimeout(_action, 1);
            return true;
        }
        return false;
    }

    protected var _address :Address;
    protected var _action :Function;
}
