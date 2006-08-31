package com.threerings.msoy.client {

import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.Canvas;

import mx.controls.Button;

import mx.events.FlexEvent;

import com.threerings.mx.controls.CommandButton;
import com.threerings.mx.controls.CommandMenu;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.chat.client.ChatControl;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends Canvas
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 59;

    public function ControlBar (ctx :MsoyContext)
    {
        _ctx = ctx;

        var fn :Function = function (event :ClientEvent) :void {
            checkControls();
        };
        _ctx.getClient().addClientObserver(
            new ClientAdapter(fn, fn, null, null, null, null, fn));

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        height = HEIGHT;

        checkControls();
    }

    /**
     * Check to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var user :MemberObject = _ctx.getClientObject();
        var isMember :Boolean = (user != null) && !user.isGuest();
        if (numChildren > 0 && (isMember == _isMember)) {
            return;
        }

        // remove all children
        while (numChildren > 0) {
            removeChildAt(0);
        }

        if (isMember) {
            [Embed(source="../../../../../../rsrc/media/uibar.png")]
            var cls :Class;
            setStyle("backgroundImage", cls);

            var chatControl :ChatControl = new ChatControl(_ctx);
            chatControl.x = 10;
            chatControl.y = 10;
            addChild(chatControl);

            // set up buttons
            var friendsBtn :CommandButton = new CommandButton();
            friendsBtn.setCommand(MsoyController.SHOW_FRIENDS);
            friendsBtn.toggle = true;

            // TODO: dynamic layout?
            friendsBtn.x = 585;
            friendsBtn.y = 0;
            friendsBtn.width = 38;
            friendsBtn.height = HEIGHT;
            addChild(friendsBtn);

            /*
            var scenesBtn :CommandButton = new CommandButton();
            scenesBtn.setCommand(MsoyController.SHOW_RECENT_SCENES);
            scenesBtn.toggle = true;
            */

            var scenesBtn :Button = new Button();
            scenesBtn.addEventListener(MouseEvent.CLICK,
                function (event :MouseEvent) :void {
                    showRoomsMenu(scenesBtn);
                });

            scenesBtn.x = 624
            scenesBtn.y = 0;
            scenesBtn.width = 38;
            scenesBtn.height = HEIGHT;
            addChild(scenesBtn);

            // set up a guest login button
            var guestBtn :CommandButton =
                new CommandButton(MsoyController.LOGON);
            guestBtn.x = 753;
            guestBtn.y = 0;
            guestBtn.width = 47;
            guestBtn.height = HEIGHT;
            addChild(guestBtn);

        } else {
            setStyle("backgroundImage", null);
            var logonPanel :LogonPanel = new LogonPanel(_ctx);
            logonPanel.x = 10;
            logonPanel.y = 10;
            addChild(logonPanel);
        }

        // and remember how things are set for now
        _isMember = isMember;
    }

    protected function showRoomsMenu (trigger :Button) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        var currentSceneId :int = (scene == null) ? -1 : scene.getId();

        var memberObj :MemberObject = _ctx.getClientObject();
        var entries :Array = memberObj.recentScenes.toArray();
        entries.sort(function (o1 :Object, o2 :Object) :int {
            var sb1 :SceneBookmarkEntry = (o1 as SceneBookmarkEntry);
            var sb2 :SceneBookmarkEntry = (o2 as SceneBookmarkEntry);
            return int(sb1.lastVisit - sb2.lastVisit);
        });

        entries = entries.map(
            function (item :*, index :int, array :Array) :Object {
                var sb :SceneBookmarkEntry = (item as SceneBookmarkEntry);
                return {
                    label: sb.toString(),
                    enabled: (sb.sceneId != currentSceneId),
                    command: MsoyController.GO_SCENE,
                    arg: sb.sceneId
                };
            });

        var menuData :Array = [
            { label: _ctx.xlate("general", "l.recent_scenes"),
              children: entries }
        ];

        if (!memberObj.isGuest()) {
            menuData.push(
                { type: "separator" },
                { label: _ctx.xlate("general", "l.go_home"),
                  enabled: (memberObj.homeSceneId != currentSceneId),
                  command :MsoyController.GO_SCENE,
                  arg: memberObj.homeSceneId
                });
        }

        var menu :CommandMenu =
            CommandMenu.createMenu(_ctx.getRootPanel(), menuData);
        menu.popUp(trigger);
    }

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;
}
}
