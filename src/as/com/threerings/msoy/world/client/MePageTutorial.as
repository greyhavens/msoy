//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

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
        _ctx.getMsoyClient().addEventListener(MsoyClient.GWT_PAGE_CHANGED, onPageChange);
    }

    protected function onPageChange (evt :ValueEvent) :void
    {
        _current = evt.value as Address;
        trace("Page changed to " + _current);

        if (_trigger != null) {
            _trigger.activate(_current);
            _trigger = null;
            return;
        }

        if (Address.ME.equals(_current)) {
            _ctx.getTutorialDirector().newSuggestion("mePageTest", "Hey, edit your profile!")
                .button("Show Me!", editProfile).queue();
        } else {
            trace("Page not changed to " + Address.ME);
        }
    }

    protected function editProfile () :void
    {
        var profile :Address = Address.profile(getMemberId());
        if (!display(profile)) {
            feedback("Problem opening the profile page");
            return;
        }

        var trigger :Trigger = new Trigger(profile, Util.adapt(PageCommand.editProfile, _ctx));
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

    protected function feedback (msg :String) :void
    {
        _ctx.getMsoyChatDirector().displayFeedback(null, msg);
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
