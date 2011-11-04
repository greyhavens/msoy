//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Rectangle;
import flash.ui.Keyboard;

import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.util.Log;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.chat.client.MuteObserver;

import com.threerings.orth.ui.PlayerList;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.PlayerEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.room.client.TrackOverlay;
import com.threerings.msoy.ui.FlyingPanel;

public class FriendsListPanel extends FlyingPanel
    implements AttributeChangeListener, MuteObserver
{
    /** The width of the popup, defined by the width of the header image. */
    public static const POPUP_WIDTH :int = 219;

    public function FriendsListPanel (ctx :WorldContext) :void
    {
        super(ctx, "Friends Online");
        _wctx = ctx;
        showCloseButton = true;
        width = POPUP_WIDTH;

        _cliObs = new ClientAdapter(null, clientDidLogon);
        _wctx.getClient().addClientObserver(_cliObs);
        _wctx.getMuteDirector().addMuteObserver(this);

        open();
    }

    override public function close () :void
    {
        var memObj :MemberObject = _wctx.getMemberObject();
        if (memObj != null) {
            memObj.removeListener(this);
        }
        if (_friendsList != null) {
            _friendsList.shutdown();
        }
        _wctx.getMuteDirector().removeMuteObserver(this);
        _wctx.getClient().removeClientObserver(_cliObs);

        super.close();
    }

    // from MuteObserver
    public function muteChanged (name :Name, nowMuted :Boolean) :void
    {
        // if we muted or unmuted a friend, refresh the display
        if ((name is MemberName) &&
                _wctx.getMemberObject().isOnlineFriend(MemberName(name).getId())) {
            _friendsList.refresh();
        }
    }

    // from AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.HEADLINE) {
            setStatus(event.getValue() as String);
        }
    }

    // part of ClientObserver, adapted by _cliObs
    protected function clientDidLogon (... ignored) :void
    {
        close();
    }

    override protected function didOpen () :void
    {
        // auto-position ourselves
        var placeBounds :Rectangle = _wctx.getTopPanel().getPlaceViewBounds();
        height = placeBounds.height - PADDING * 2 - TrackOverlay.HEIGHT;
        x = placeBounds.right - width - PADDING;
        y = placeBounds.y + PADDING;

        super.didOpen();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        if (_wctx.getMemberObject().isPermaguest()) {
            var joinBtn :CommandButton = new CommandButton(null, MsoyController.SHOW_SIGN_UP);
            joinBtn.styleName = "joinNowButton";
            addChild(FlexUtil.createWideText(Msgs.GENERAL.get("m.guest_friends"), "playerLabel"));
            addChild(joinBtn);
            return;
        }

        _friendsList = new PlayerList(FriendRenderer.createFactory(_wctx), PlayerEntry.sortByName);
        addChild(_friendsList);

        // add a little separator
        var separator :VBox = new VBox();
        separator.percentWidth = 100;
        separator.height = 1;
        separator.styleName = "panelBottomSeparator";
        addChild(separator);

        // add the little box at the bottom
        var box :VBox = new VBox();
        box.percentWidth = 100;
        box.styleName = "panelBottom";
        addChild(box);

        // Create a display name label and a status editor
        var me :MemberObject = _wctx.getMemberObject();
        _nameLabel = new Label();
        _nameLabel.styleName = "playerLabel";
        _nameLabel.setStyle("fontWeight", "bold");
        _nameLabel.text = me.memberName.toString();
        box.addChild(_nameLabel);
        _statusEdit = new TextInput();
        _statusEdit.editable = true;
        setStatus(me.headline);
        _statusEdit.styleName = "statusEdit";
        _statusEdit.percentWidth = 100;
        _statusEdit.height = 17;
        _statusEdit.maxChars = PROFILE_MAX_STATUS_LENGTH;
        _statusEdit.addEventListener(MouseEvent.MOUSE_OVER, editMouseOver);
        _statusEdit.addEventListener(MouseEvent.MOUSE_OUT, editMouseOut);
        _statusEdit.addEventListener(FocusEvent.FOCUS_IN, editFocusIn);
        _statusEdit.addEventListener(FocusEvent.FOCUS_OUT, editFocusOut);
        _statusEdit.addEventListener(MouseEvent.CLICK, editMouseOut);
        _statusEdit.addEventListener(FlexEvent.ENTER, commitEdit);
        _statusEdit.addEventListener(KeyboardEvent.KEY_UP, keyUp);
        box.addChild(_statusEdit);

        // initialize with currently online friends
        init(me);
    }

    protected function init (memObj :MemberObject) :void
    {
        memObj.addListener(this);
        _friendsList.init(memObj, MemberObject.FRIENDS);
    }

    protected function editMouseOver (...ignored) :void
    {
        _statusEdit.styleName = "statusEditHover";
    }

    protected function editMouseOut (...ignored) :void
    {
        _statusEdit.styleName = "statusEdit";
    }

    protected function editFocusIn (...ignored) :void
    {
        if (_statusEdit.text == Msgs.GENERAL.get("l.emptyStatus")) {
            _statusEdit.text = Msgs.GENERAL.get("l.statusPrompt");
        } else {
            // highlight everything in there so you can just type in your new status
            var selectionEnd :int = _statusEdit.text == null ? 0 : _statusEdit.text.length;
            _statusEdit.setSelection(0, selectionEnd);
        }
    }

    protected function editFocusOut (...ignored) :void
    {
        // quick check
        if (_statusEdit.text == Msgs.GENERAL.get("l.statusPrompt") ||
            _statusEdit.text == "") {
            setStatus("");
        }
    }

    protected function commitEdit (...ignored) :void
    {
        _statusEdit.setSelection(0, 0);
        // delay losing focus by a frame so the selection has time to get set correctly.
        callLater(_wctx.getControlBar().giveChatFocus);
        var newStatus :String = StringUtil.deNull(_wctx.getChatDirector().filter(
            StringUtil.trim(_statusEdit.text), null, true));
        if (newStatus != _wctx.getMemberObject().headline) {
            var msvc :MemberService =
                (_wctx.getClient().requireService(MemberService) as MemberService);
            msvc.updateStatus(newStatus, new InvocationAdapter(
                function (cause :String) :void {
                    _wctx.displayFeedback(null, cause);
                    // revert to old status
                    var me :MemberObject = _wctx.getMemberObject();
                    setStatus(me.headline);
                }));
        }
    }

    protected function keyUp (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ESCAPE) {
            var me :MemberObject = _wctx.getMemberObject();
            setStatus(me.headline);
            _statusEdit.setSelection(0, 0);
            // delay losing focus by a frame so the selection has time to get set correctly.
            callLater(_wctx.getControlBar().giveChatFocus);
        }
    }

    protected function setStatus (status :String) :void
    {
        _statusEdit.text = StringUtil.isBlank(status) ? Msgs.GENERAL.get("l.emptyStatus")
            : StringUtil.deNull(_wctx.getChatDirector().filter(status, null, false));
    }

    private static const log :Log = Log.getLog(FriendsListPanel);

    /** Defined in Java as com.threerings.msoy.person.data.Profile.MAX_STATUS_LENGTH */
    protected static const PROFILE_MAX_STATUS_LENGTH :int = 100;

    protected var _wctx :WorldContext;

    protected var _cliObs :ClientAdapter;
    protected var _friendsList :PlayerList;
    protected var _nameLabel :Label;
    protected var _statusEdit :TextInput;
}
}
