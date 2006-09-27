package com.threerings.msoy.client {

import mx.controls.Button;

import com.threerings.util.Controller;
import com.threerings.util.Name;

import com.threerings.mx.controls.CommandMenu;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneObject;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.SceneBookmarkEntry;

public class MsoyController extends Controller
    implements ClientObserver
{
    public static const log :Log = Log.getLog(MsoyController);

    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to display the friends list. */
    public static const SHOW_FRIENDS :String = "ShowFriends";

    /** Command to display the recent scenes list. */
    public static const POP_ROOMS_MENU :String = "PopRoomsMenu"

    /** Command to go to a particular scene. */
    public static const GO_SCENE :String = "GoScene";

    /** Command to go to a friend's home scene. */
    public static const GO_FRIEND_HOME :String = "GoFriendHome";

    /**
     * Create the msoy controller.
     */
    public function MsoyController (ctx :MsoyContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(this);
        _topPanel = topPanel;
        setControlledPanel(ctx.getRootPanel());
    }

    /**
     * Handle the SHOW_FRIENDS command.
     */
    public function handleShowFriends (show :Boolean) :void
    {
        _topPanel.showFriends(show);
    }

    /**
     * Handle the POP_ROOMS_MENU command.
     */
    public function handlePopRoomsMenu (trigger :Button) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        var currentSceneId :int = (scene == null) ? -1 : scene.getId();
        if (!(_ctx.getLocationDirector().getPlaceObject() is SceneObject)) {
            currentSceneId = -1;
        }

        var memberObj :MemberObject = _ctx.getClientObject();

        var friends :Array = memberObj.friends.toArray();
        friends.sort(function (o1 :Object, o2 :Object) :int {
            var fe1 :FriendEntry = (o1 as FriendEntry);
            var fe2 :FriendEntry = (o2 as FriendEntry);
            return MemberName.BY_DISPLAY_NAME(fe1.name, fe2.name);
        });

        friends = friends.map(
            function (item :*, index :int, array :Array) :Object {
                var fe :FriendEntry = (item as FriendEntry);
                return {
                    label: fe.name.toString(),
                    command: GO_FRIEND_HOME,
                    arg: fe.getMemberId()
                };
            });

        var recent :Array = memberObj.recentScenes.toArray();
        recent.sort(function (o1 :Object, o2 :Object) :int {
            var sb1 :SceneBookmarkEntry = (o1 as SceneBookmarkEntry);
            var sb2 :SceneBookmarkEntry = (o2 as SceneBookmarkEntry);
            return int(sb1.lastVisit - sb2.lastVisit);
        });

        recent = recent.map(
            function (item :*, index :int, array :Array) :Object {
                var sb :SceneBookmarkEntry = (item as SceneBookmarkEntry);
                return {
                    label: sb.toString(),
                    enabled: (sb.sceneId != currentSceneId),
                    command: GO_SCENE,
                    arg: sb.sceneId
                };
            });

        var menuData :Array = [];

        if (friends.length > 0) {
            menuData.push({ label: _ctx.xlate("general", "l.visit_friends"),
                children: friends });
        }
        menuData.push({ label: _ctx.xlate("general", "l.recent_scenes"),
            children: recent });

        if (!memberObj.isGuest()) {
            menuData.push(
                { type: "separator" },
                { label: _ctx.xlate("general", "l.go_home"),
                  enabled: (memberObj.homeSceneId != currentSceneId),
                  command :GO_SCENE,
                  arg: memberObj.homeSceneId
                });
        }

        var menu :CommandMenu =
            CommandMenu.createMenu(_ctx.getRootPanel(), menuData);
        menu.popUp(trigger);
    }

    /**
     * Handle the GO_SCENE command.
     */
    public function handleGoScene (sceneId :int) :void
    {
        _ctx.getSceneDirector().moveTo(sceneId);
    }

    /**
     * Handle the GO_FRIEND_HOME command.
     */
    public function handleGoFriendHome (memberId :int) :void
    {
        var msvc :MemberService =
            (_ctx.getClient().requireService(MemberService) as MemberService);
        msvc.getMemberHomeId(_ctx.getClient(), memberId, new ResultWrapper(
            function (cause :String) :void {
                log.warning("Unable to go to friend's home: " + cause);
            },
            function (sceneId :int) :void {
                _ctx.getSceneDirector().moveTo(sceneId);
            }));
    }

    /**
     * Handle the LOGON command.
     */
    public function handleLogon (creds :MsoyCredentials) :void
    {
        _ctx.getClient().logoff(false);
        _topPanel.callLater(function () :void {
            var client :Client = _ctx.getClient();
            if (creds == null) {
                creds = new MsoyCredentials(null, null);
                creds.ident = "";
            }
            client.setCredentials(creds);
            client.logon();
        });
    }

    // from ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        var memberObj :MemberObject = _ctx.getClientObject();
        // if not a guest, save the username that we logged in with
        if (!memberObj.isGuest()) {
            var creds :MsoyCredentials =
                (_ctx.getClient().getCredentials() as MsoyCredentials);
            var name :Name = creds.getUsername();
            if (name != null) {
                Prefs.setUsername(name.toString());
            }
        }

        // TODO
        // for now, all we do is move to a starter scene
        var starterSceneId :int = memberObj.homeSceneId;
        if (starterSceneId == 0) {
            starterSceneId = 1; // for "packwards combatability"
        }
        _ctx.getSceneDirector().moveTo(starterSceneId);
    }

    // from ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        _topPanel.setPlaceView(new DisconnectedPanel(_ctx, _logoffMessage));
        _logoffMessage = null;
    }

    // from ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        _logoffMessage = _ctx.xlate(null, "m.lost_connection");
    }

    // from ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        // nada
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :MsoyContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;
}
}
