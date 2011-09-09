//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;

import mx.collections.ArrayCollection;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.HorizontalList;
import mx.core.ClassFactory;
import mx.core.ScrollPolicy;
import mx.events.CollectionEvent;

import com.threerings.util.Comparators;
import com.threerings.util.F;

import com.threerings.flex.CommandButton;
import com.threerings.flex.DSetList;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.ui.MsoyAudioDisplay;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

public class DjList extends VBox
{
    public static const MAX_DJS :int = 4;

    public function DjList (ctx :WorldContext, roomObj :RoomObject)
    {
        _ctx = ctx;
        _roomObj = roomObj;

        var cf :ClassFactory = new ClassFactory(DjPanel);
        cf.properties = { wctx: _ctx, roomObj: _roomObj };
        _list = new HorizontalList();
        _list.itemRenderer = cf;
        _list.verticalScrollPolicy = _list.horizontalScrollPolicy = ScrollPolicy.OFF;
        _list.maxWidth = MsoyAudioDisplay.WIDTH;
        _list.selectable = false;
        _list.setStyle("borderStyle", "none");

        // Steal DSetList's shiny dataProvider
        var dsetList :DSetList = new DSetList(null, Comparators.compareComparables);
        _list.dataProvider = dsetList.dataProvider;
        dsetList.dataProvider = null;

        addEventListener(Event.ADDED_TO_STAGE, function (..._) :void {
            dsetList.init(_roomObj, RoomObject.DJS, RoomObject.CURRENT_DJ);
        });
        addEventListener(Event.REMOVED_FROM_STAGE, F.adapt(dsetList.shutdown));

        var rightBox :HBox = new HBox();
        rightBox.percentWidth = 100;
        rightBox.setStyle("horizontalAlign", "right");
        _addButton = new CommandButton(Msgs.WORLD.get("b.add_music"),
            WorldController.VIEW_STUFF, Item.AUDIO);
        rightBox.addChild(_addButton);
        rightBox.addChild(FlexUtil.createSpacer());

        _titleBox = new HBox();
        _titleBox.percentWidth = 100;
        _titleBox.addChild(FlexUtil.createLabel(
            Msgs.WORLD.get("l.playlist_dj"), "playlistTitle"));
        _titleBox.addChild(rightBox);
        addChild(_titleBox);

        ArrayCollection(_list.dataProvider).addEventListener(
            CollectionEvent.COLLECTION_CHANGE, onListChange);
    }

    override protected function createChildren () :void
    {
        addChild(_list);
        addChild(_titleBox);

        // HACK: Can't get percentWidth working properly, fixed width for now
        this.width = MsoyAudioDisplay.WIDTH;
    }

    protected function onListChange (..._) :void
    {
        var amDj :Boolean = _roomObj.djs.containsKey(_ctx.getMyId());
        _titleBox.setStyle("paddingBottom", amDj ? 0: 8);
        _addButton.enabled = amDj || (_list.dataProvider.length < MAX_DJS);
    }

    protected var _ctx :WorldContext;
    protected var _roomObj :RoomObject;
    protected var _list :HorizontalList;
    protected var _addButton :CommandButton;
    protected var _titleBox :HBox;
}
}

import flash.events.MouseEvent;

import mx.containers.Canvas;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.util.CommandEvent;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.MediaWrapper;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.room.client.DjList;
import com.threerings.msoy.room.data.Deejay;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.ui.MsoyAudioDisplay;
import com.threerings.msoy.world.client.WorldContext;

class DjPanel extends VBox
{
    public static const WIDTH :int = MsoyAudioDisplay.WIDTH / DjList.MAX_DJS;

    public var wctx :WorldContext;
    public var roomObj :RoomObject;

    public function DjPanel ()
    {
        var box :Canvas = new Canvas();
        box.addChild(_headShot = MediaWrapper.createView(null, MediaDescSize.HALF_THUMBNAIL_SIZE));
        _headShot.x = WIDTH/2 - 0.25*MediaDescSize.THUMBNAIL_WIDTH;

        box.addChild(_bootBtn = new CommandButton());
        _bootBtn.styleName = "closeButton";
        _bootBtn.x = _headShot.x + 0.5*MediaDescSize.THUMBNAIL_WIDTH - 8;
        _bootBtn.y = 0;
        addChild(box);

        addChild(_name = FlexUtil.createLabel("", "nameLabel"));
        _name.width = WIDTH;
        _name.setStyle("textAlign", "center");

        addEventListener(MouseEvent.CLICK, onClick);
    }

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }

        var dj :Deejay = Deejay(value);
        _info = roomObj.getMemberInfo(dj.memberId);

        _headShot.setMediaDesc(VizMemberName(_info.username).getPhoto());
        _name.text = _info.username.toString();
        if (dj.memberId == wctx.getMyId()) {
            _bootBtn.visible = true;
            _bootBtn.setCallback(function () :void {
                roomObj.roomService.quitDjing();
            });
        } else {
            _bootBtn.visible = wctx.getWorldController().canManagePlace();
            _bootBtn.setCallback(function () :void {
                roomObj.roomService.bootDj(dj.memberId, wctx.listener());
            });
        }

        setStyle("backgroundColor", (dj.memberId == roomObj.currentDj) ? 0x54a9da : undefined);
    }

    protected function onClick (..._) :void
    {
        CommandEvent.dispatch(this, MsoyController.POP_MEMBER_MENU,
            [ _info.username, _info.getMemberId() ]);
    }

    protected var _info :MemberInfo;
    protected var _headShot :MediaWrapper;
    protected var _name :Label;
    protected var _bootBtn :CommandButton;
}
