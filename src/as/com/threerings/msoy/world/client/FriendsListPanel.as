//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.ui.Keyboard;

import flash.utils.Dictionary;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.containers.VBox;
import mx.containers.TitleWindow;

import mx.controls.Label;
import mx.controls.List;
import mx.controls.TextInput;

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import mx.events.FlexEvent;

import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.ui.FlyingPanel;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PeerList;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

public class FriendsListPanel extends FlyingPanel
    implements AttributeChangeListener
{
    /** The width of the popup, defined by the width of the header image. */
    public static const POPUP_WIDTH :int = 219;

    public function FriendsListPanel (ctx :WorldContext) :void
    {
        super(ctx);
        showCloseButton = true;
        open();

        _cliObs = new ClientAdapter(null, clientDidLogon);
        _ctx.getClient().addClientObserver(_cliObs);

        // TODO: automatically pop-down when you enter a game ??? (Used to work, but do we want it?)
    }

    override public function close () :void
    {
        _ctx.getMemberObject().removeListener(this);
        _ctx.getMemberObject().removeListener(_friendsList);
        _ctx.getClient().removeClientObserver(_cliObs);

        super.close();
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
        var memObj :MemberObject = _ctx.getMemberObject();
        if (memObj.isGuest()) {
            close();
        } else {
            init(memObj); // member object changed!
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // styles and positioning
        styleName = "friendsListPanel";
        width = POPUP_WIDTH;
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds(); 
        height = placeBounds.height - PADDING * 2;
        x = placeBounds.x + placeBounds.width - width - PADDING;
        y = placeBounds.y + PADDING;

        _friendsList = new PeerList(_ctx, MemberObject.FRIENDS, FriendRenderer);
        _friendsList.dataProvider.filterFunction = function (friend :FriendEntry) :Boolean {
            // Only show online friends
            return friend.online;
        }

        addChild(_friendsList);

        // add a little separator
        var separator :VBox = new VBox();
        separator.percentWidth = 100;
        separator.height = 1;
        separator.styleName = "friendsListSeparator";
        addChild(separator);

        // add the little box at the bottom
        var box :VBox = new VBox();
        box.percentWidth = 100;
        box.styleName = "friendsListEditorBox";
        addChild(box);

        // Create a display name label and a status editor
        var me :MemberObject = _ctx.getMemberObject();
        _nameLabel = new Label();
        _nameLabel.styleName = "friendLabel";
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
        memObj.addListener(_friendsList);

        _friendsList.init(memObj.friends.toArray());
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
        callLater(function () :void { _ctx.getTopPanel().getControlBar().giveChatFocus(); });
        var newStatus :String = _statusEdit.text;
        if (newStatus != _ctx.getMemberObject().headline) {
            var msvc :MemberService =
                (_ctx.getClient().requireService(MemberService) as MemberService);
            msvc.updateStatus(_ctx.getClient(), newStatus, new InvocationAdapter(
                function (cause :String) :void {
                    _ctx.displayFeedback(null, cause);
                    // revert to old status
                    var me :MemberObject = _ctx.getMemberObject();
                    setStatus(me.headline);
                }));
        }
    }

    protected function keyUp (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ESCAPE) {
            var me :MemberObject = _ctx.getMemberObject();
            setStatus(me.headline);
            _statusEdit.setSelection(0, 0);
            // delay losing focus by a frame so the selection has time to get set correctly.
            callLater(_ctx.getTopPanel().getControlBar().giveChatFocus);
        }
    }

    protected function setStatus (status :String) :void
    {
        _statusEdit.text = 
            status == "" || status == null ?  Msgs.GENERAL.get("l.emptyStatus") : status;
    }

    private static const log :Log = Log.getLog(FriendsListPanel);

    protected static const PADDING :int = 10;

    /** Defined in Java as com.threerings.msoy.person.data.Profile.MAX_STATUS_LENGTH */
    protected static const PROFILE_MAX_STATUS_LENGTH :int = 100;

    protected var _cliObs :ClientAdapter;
    protected var _friendsList :PeerList;
    protected var _nameLabel :Label;
    protected var _statusEdit :TextInput;
}
}
