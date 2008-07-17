//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

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
import mx.core.mx_internal;

import mx.events.CloseEvent;
import mx.events.FlexEvent;

import mx.managers.PopUpManager;

import com.threerings.presents.client.InvocationAdapter;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.flex.PopUpUtil;

import com.threerings.util.Log;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

public class FriendsListPanel extends TitleWindow
    implements SetListener, AttributeChangeListener
{
    /** The width of the popup, defined by the width of the header image. */
    public static const POPUP_WIDTH :int = 219;

    public function FriendsListPanel (ctx :WorldContext) :void
    {
        _ctx = ctx;

        addEventListener(CloseEvent.CLOSE, _ctx.getWorldController().handlePopFriendsList);
    }

    public function show () :void
    {
        PopUpManager.addPopUp(this, _ctx.getTopPanel(), false);
        systemManager.addEventListener(Event.RESIZE, stageResized);
    }

    public function shutdown () :void
    {
        systemManager.removeEventListener(Event.RESIZE, stageResized);
        _ctx.getMemberObject().removeListener(this);
        PopUpManager.removePopUp(this);
    }

    public function memberObjectUpdated (memObj :MemberObject) :void
    {
        init(memObj);
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            addFriend(event.getEntry() as FriendEntry);
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            var newEntry :FriendEntry = event.getEntry() as FriendEntry;
            var oldEntry :FriendEntry = event.getOldEntry() as FriendEntry;
            removeFriend(oldEntry);
            if (newEntry.online) {
                addFriend(newEntry);
            }
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == MemberObject.FRIENDS) {
            removeFriend(event.getOldEntry() as FriendEntry);
        }
    }

    // from AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.HEADLINE) {
            _statusEdit.text = event.getValue() as String;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // Panel provides no way to customize this, other than overriding the class and blowing
        // away what it set these to.
        mx_internal::closeButton.explicitWidth = 13;
        mx_internal::closeButton.explicitHeight = 14;

        // styles and positioning
        styleName = "friendsListPanel";
        showCloseButton = true;
        width = POPUP_WIDTH;
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds(); 
        height = placeBounds.height - PADDING * 2;
        x = placeBounds.x + placeBounds.width - width - PADDING;
        y = placeBounds.y + PADDING;

        _friendsList = new List();
        _friendsList.styleName = "friendList";
        _friendsList.horizontalScrollPolicy = ScrollPolicy.OFF;
        // I'd love to make this AUTO, but labels are stupid and can't deal with growing only
        // up to a dynamic size, so I've got to make all the widths static.  
        _friendsList.verticalScrollPolicy = ScrollPolicy.ON;
        _friendsList.percentWidth = 100;
        _friendsList.percentHeight = 100;
        _friendsList.itemRenderer = new ClassFactory(FriendRenderer);
        _friendsList.dataProvider = _friends;
        _friendsList.selectable = false;
        _friendsList.variableRowHeight = true;
        addChild(_friendsList);

        // set up the sort for the collection
        var sort :Sort = new Sort();
        sort.compareFunction = sortFunction;
        _friends.sort = sort;
        _friends.refresh();

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
        _statusEdit.text = me.headline == "" || me.headline == null ? 
            Msgs.GENERAL.get("l.emptyStatus") : me.headline;
        _statusEdit.styleName = "statusEdit";
        _statusEdit.percentWidth = 100;
        _statusEdit.height = 17;
        _statusEdit.maxChars = PROFILE_MAX_STATUS_LENGTH;
        _statusEdit.addEventListener(MouseEvent.MOUSE_OVER, editMouseOver);
        _statusEdit.addEventListener(MouseEvent.MOUSE_OUT, editMouseOut);
        _statusEdit.addEventListener(FocusEvent.FOCUS_IN, editFocusIn);
        _statusEdit.addEventListener(MouseEvent.CLICK, editMouseOut);
        _statusEdit.addEventListener(FlexEvent.ENTER, commitEdit);
        box.addChild(_statusEdit);
    
        // initialize with currently online friends
        init(me);
    }

    override protected function layoutChrome (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.layoutChrome(unscaledWidth, unscaledHeight);

        mx_internal::closeButton.x = POPUP_WIDTH - mx_internal::closeButton.width - 5;
        mx_internal::closeButton.y = 5;
    }

    protected function init (memObj :MemberObject) :void
    {
        memObj.addListener(this);

        var currentEntries :Array = _friends.toArray();
        for each (var friend :FriendEntry in memObj.friends.toArray()) {
            if (!friend.online) {
                continue;
            }

            if (!containsFriend(friend)) {
                addFriend(friend);
            } else {
                var original :Object = _originals[friend.name.getMemberId()];
                currentEntries.splice(currentEntries.indexOf(original), 1);
            }
        }

        // anything left in currentEntries wasn't found on the new MemberObject
        for each (var entry :Array in currentEntries) {
            removeFriend(entry[1] as FriendEntry);
        }
    }

    protected function sortFunction (o1 :Object, o2 :Object, fields :Array = null) :int
    {
        if (!(o1 is Array) || !(o2 is Array)) {
            return 0;
        }

        var friend1 :FriendEntry = (o1 as Array)[1] as FriendEntry;
        var friend2 :FriendEntry = (o2 as Array)[1] as FriendEntry;
        return MemberName.BY_DISPLAY_NAME(friend1.name, friend2.name);
    }

    protected function containsFriend (friend :FriendEntry) :Boolean
    {
        return _originals[friend.name.getMemberId()] !== undefined;
    }

    protected function addFriend (friend :FriendEntry) :void
    {
        var data :Array = [ _ctx, friend ];
        _originals[friend.name.getMemberId()] = data;
        _friends.addItem(data);
    }

    protected function removeFriend (friend :FriendEntry) :void
    {
        var data :Array = _originals[friend.name.getMemberId()] as Array;
        if (data != null) {
            _friends.removeItemAt(_friends.getItemIndex(data));
        }
        delete _originals[friend.name.getMemberId()];
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
        }
    }

    protected function commitEdit (...ignored) :void
    {
        _ctx.getTopPanel().getControlBar().giveChatFocus();
        var newStatus :String = _statusEdit.text;
        if (newStatus != _ctx.getMemberObject().headline) {
            var msvc :MemberService =
                (_ctx.getClient().requireService(MemberService) as MemberService);
            msvc.updateStatus(_ctx.getClient(), newStatus, new InvocationAdapter(
                function (cause :String) :void {
                    _ctx.displayFeedback(null, cause);
                    // revert to old status
                    var me :MemberObject = _ctx.getMemberObject();
                    _statusEdit.text = me.headline == "" || me.headline == null ?
                        Msgs.GENERAL.get("l.emptyStatus") : me.headline;
                }));
        }
    }

    protected function stageResized (...ignored) :void
    {
        var placeBounds :Rectangle = _ctx.getTopPanel().getPlaceViewBounds(); 
        // fix the height
        height = placeBounds.height - PADDING * 2;
        // fit the popup within the new bounds, minux padding.
        placeBounds.x += PADDING;
        placeBounds.y += PADDING;
        placeBounds.width -= PADDING * 2;
        placeBounds.height -= PADDING * 2;
        PopUpUtil.fitInRect(this, placeBounds);
    }

    private static const log :Log = Log.getLog(FriendsListPanel);

    protected static const PADDING :int = 10;

    /** Defined in Java as com.threerings.msoy.person.data.Profile.MAX_STATUS_LENGTH */
    protected static const PROFILE_MAX_STATUS_LENGTH :int = 100;

    protected var _ctx :WorldContext;
    protected var _friendsList :List;
    protected var _friends :ArrayCollection = new ArrayCollection();
    protected var _originals :Dictionary = new Dictionary();
    protected var _nameLabel :Label;
    protected var _statusEdit :TextInput;
}
}
