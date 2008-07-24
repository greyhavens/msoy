//
// $Id: FriendsListPanel.as 10001 2008-07-24 00:41:36Z bruno $

package com.threerings.msoy.party.client {

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
import com.threerings.util.ValueEvent;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.PeerList;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.WorldContext;

public class PartyPopup extends TitleWindow
{
    /** The width of the popup, defined by the width of the header image. */
    public static const POPUP_WIDTH :int = 219;

    public function PartyPopup (ctx :WorldContext) :void
    {
        _ctx = ctx;
        _ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        addEventListener(CloseEvent.CLOSE, _ctx.getWorldController().handlePopParty);
    }

    public function show () :void
    {
        PopUpManager.addPopUp(this, _ctx.getTopPanel(), false);
        systemManager.addEventListener(Event.RESIZE, stageResized);
    }

    public function shutdown () :void
    {
        systemManager.removeEventListener(Event.RESIZE, stageResized);
        _ctx.getMemberObject().removeListener(_friendsList);
        PopUpManager.removePopUp(this);
    }

    public function memberObjectUpdated (memObj :MemberObject) :void
    {
        init(memObj);
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

        _friendsList = new PeerList(_ctx, MemberObject.FRIENDS, PartyRenderer);

        addChild(_friendsList);

        var me :MemberObject = _ctx.getMemberObject();
        /*// add a little separator
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
        box.addChild(_statusEdit);*/
    
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
        memObj.addListener(_friendsList);

        _friendsList.init(memObj.friends.toArray());
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

    protected function miniWillChange (event :ValueEvent) :void
    {
        if (event.value) {
            _currentX = x;
        } else {
            x = _currentX;
        }
    }

    private static const log :Log = Log.getLog(PartyPopup);

    protected static const PADDING :int = 10;

    /** Defined in Java as com.threerings.msoy.person.data.Profile.MAX_STATUS_LENGTH */
    protected static const PROFILE_MAX_STATUS_LENGTH :int = 100;

    protected var _ctx :WorldContext;
    protected var _friendsList :PeerList;
    protected var _friends :ArrayCollection = new ArrayCollection();
    protected var _nameLabel :Label;
    protected var _statusEdit :TextInput;
    protected var _currentX :int = 0;
}
}
