// 
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;

import mx.events.ItemClickEvent;

import mx.collections.ArrayCollection;

import flexlib.controls.SuperTabBar;

import flexlib.controls.tabBarClasses.SuperTab;

/**
 * SuperTabBar doesn't leave any way of notifying its creator when a tab is closed, so since we
 * need that information, we have to extend it and do it ourselves.
 */
public class HeaderChatTabBar extends SuperTabBar
{
    public function HeaderChatTabBar ()
    {
        super();

        closePolicy = SuperTab.CLOSE_NEVER;
        dataProvider = _tabNames;
        dragEnabled = false;
        dropEnabled = false;
        addEventListener(ItemClickEvent.ITEM_CLICK, tabSelected);
    }

    public function getLocationName () :String
    {
        if (_tabNames.length == 0) {
            return null;
        }
        return _tabNames.getItemAt(0) as String;
    }

    public function setLocationName (name :String) :void
    {
        if (_tabNames.length == 0) {
            _tabNames.addItem(name);
        } else {
            _tabNames.setItemAt(name, 0);
        }
        selectedIndex = 0;
        tabSelected();
    }

    public function addChatTab (name :String) :void
    {
        _tabNames.addItem(name);
        selectedIndex = _tabNames.length - 1;
        tabSelected();
    }

    /** 
     * Thanks a lot flex team - you've got at least two classes in this hierarchy with
     * _selectedIndex variables that mean exactly the same damn thing, and they're both private...
     * so I have to make a third one.
     */
    override public function set selectedIndex (ii :int) :void
    {
        super.selectedIndex = ii;
        _selectedIndex = ii;
    }

    // yay for this not actually being private like they labeled it in the docs.
    override public function onCloseTabClicked (event :Event) :void
    {
        super.onCloseTabClicked(event);

        // default back to room chat when a tab is closed
        selectedIndex = 0;
        tabSelected();

        // TODO: close chat channel
    }

    protected function tabSelected (event :ItemClickEvent = null) :void
    {
        var index :int = event == null ? _selectedIndex : event.index;
        // this is a stupid hack, but it seems to be the only way to get "Super"TabNav to actually
        // do what's its supposed to and allow some tabs to be closeable and others not.
        closePolicy = index == 0 ? SuperTab.CLOSE_NEVER : SuperTab.CLOSE_SELECTED;
        Log.getLog(this).debug("selected tab [" + _tabNames.getItemAt(index) + "]");

        // TODO: update chat history shown on the chat overlay
    }

    protected var _tabNames :ArrayCollection = new ArrayCollection;

    // The value returned from get selectedIndex() does not always reflect the value that was 
    // just immeadiately set via set selectedIndex(), so lets keep track of what we really want.
    protected var _selectedIndex :int = -1;
}
}
