//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;

import mx.core.UIComponent;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;

import mx.events.ChildExistenceChangedEvent;
import mx.events.FlexEvent;
import mx.utils.StringUtil;

import flexlib.containers.SuperTabNavigator;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.flex.ChatInput;
import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;
import com.threerings.util.HashMap;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Displays our various chat channels in tabs.
 */
public class ChatChannelPanel extends VBox
{
    public function ChatChannelPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        width = TopPanel.RIGHT_SIDEBAR_WIDTH;

        addChild(_tabnav = new ChatTabNavigator());
        _tabnav.closePolicy = SuperTab.CLOSE_SELECTED; // can't do this in the constructor, yay!
        _tabnav.percentWidth = 100;
        _tabnav.percentHeight = 100;
        _tabnav.addEventListener(ChildExistenceChangedEvent.CHILD_REMOVE, tabRemoved);
    }

    public function displayGameChat (chatDtr :ChatDirector, playerList :UIComponent) :void
    {
        _gtab = new GameChatTab(_ctx, chatDtr, playerList);
        _gtab.label = Msgs.CHAT.xlate("l.game_channel");
        addFixedTab(_gtab);
    }

    public function clearGameChat () :void
    {
        if (_gtab != null) {
            _gtab.shutdown();
            removeFixedTab(_gtab);
            _gtab = null;
        }
    }

    protected function addFixedTab (tab :ChatTab) :void
    {
        _tabnav.addChildAt(tab, 0);
        // another action that seems to need to be deferred.
        callLater(function () :void {
            _tabnav.setClosePolicyForTab(0, SuperTab.CLOSE_NEVER);
        });
        // select the world tab if we're not showing at all, otherwise don't fiddle with the
        // selected tab to avoid hiding an active chat channel or the tutorial
        if (parent == null) {
            selectTab(tab);
        }
        _ctx.getTopPanel().getControlBar().setTabMode(true);
    }

    protected function removeFixedTab (tab :ChatTab) :void
    {
        _ctx.getTopPanel().getControlBar().setTabMode(false);
        _tabnav.removeChild(tab);
    }

    protected function selectTab (tab :ChatTab, select :Boolean = true) :void
    {
        // select this tab if requested
        if (select) {
            // We need to defer the action of selecting a tab.  If the tab is newly added,
            // attempting to select it immediately does not work, for some wacky reason.
            callLater(function () :void {
                _tabnav.selectedChild = tab;
            });
        }

        // if we're not visible, add ourselves
        if (parent == null) {
            _ctx.getTopPanel().setRightPanel(this);
        }
    }

    protected function tabRemoved (event :ChildExistenceChangedEvent) :void
    {
        if (event.relatedObject is ChatTab) {
            if (_tabnav.numChildren == 1) {
                _ctx.getTopPanel().clearRightPanel(this);
            }
        }
    }

    protected var _ctx :WorldContext;

    protected var _tabnav :SuperTabNavigator;
    protected var _gtab :GameChatTab;
}
}
