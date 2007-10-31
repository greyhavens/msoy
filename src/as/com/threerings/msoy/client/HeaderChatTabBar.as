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
        closePolicy = SuperTab.CLOSE_NEVER;
        selectedIndex = 0;
    }

    public function addChatTab (name :String) :void
    {
        _tabNames.addItem(name);
        closePolicy = SuperTab.CLOSE_SELECTED;
        selectedIndex = _tabNames.length - 1;
    }

    // yay for this not actually being private like they labeled it in the docs.
    override public function onCloseTabClicked (event :Event) :void
    {
        super.onCloseTabClicked(event);

        // TODO: close chat channel
    }

    protected function tabSelected (event :ItemClickEvent) :void 
    {
        // this is a stupid hack, but it seems to be the only way to get "Super"TabNav to actually
        // do what's its supposed to and allow some tabs to be closeable and others not.
        closePolicy = event.index == 0 ? SuperTab.CLOSE_NEVER : SuperTab.CLOSE_SELECTED;

        // TODO: update chat history shown on the chat overlay
    }

    protected var _tabNames :ArrayCollection = new ArrayCollection;
}
}
