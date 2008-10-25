//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.Event;

import mx.containers.HBox;

import mx.controls.Spacer;
import mx.controls.TextInput;
import mx.controls.ToggleButtonBar;

import mx.events.FlexEvent;
import mx.events.ItemClickEvent;

import com.threerings.flex.CommandButton;

import com.threerings.util.Log;

import com.threerings.presents.client.ResultAdapter;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MemberService;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Displays details about the room.
 */
public class RoomPanel extends BasePanel
{
    public function RoomPanel (controller :RoomEditorController)
    {
        super(controller);
    }

    // @Override from BasePanel
    override public function updateDisplay (data :FurniData) :void
    {
        super.updateDisplay(data);

        // ignore furni data - we don't care about which furni is selected,
        // only care about the room itself

        if (_controller.scene != null) {
            _name.text = _controller.scene.getName();
            updateAccessButtons();
            this.enabled = true; // override base changes
        }
    }

    public function setHomeButtonEnabled (enabled :Boolean) :void
    {
        if (_homeButton != null) {
            _homeButton.enabled = enabled;
        }
    }

    // @Override from superclass
    override protected function createChildren () :void
    {
        super.createChildren();

        // container for name
        var box :HBox = new HBox();
        box.setStyle("horizontalGap", 4);
        box.percentWidth = 100;
        addChild(box);

        _name = new TextInput();
        _name.percentWidth = 100;
        _name.maxWidth = 200;
        _name.maxChars = MsoyCodes.MAX_NAME_LENGTH;
        box.addChild(_name);

        // container for other buttons
        box = new HBox();
        box.setStyle("horizontalGap", 4);
        box.percentWidth = 100;
        addChild(box);

        // make this room my/this whirled's home button
        _homeButton = new CommandButton();
        _homeButton.styleName = "roomEditButtonMakeMyHome";
        _homeButton.setCallback(_controller.makeHome);
        var sceneModel :MsoySceneModel = _controller.scene.getSceneModel() as MsoySceneModel;
        var memberObject :MemberObject = _controller.ctx.getMemberObject();
        if (sceneModel.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
            _homeButton.toolTip = Msgs.EDITING.get("b.make_home");
            _homeButton.enabled = 
                sceneModel.ownerId == memberObject.getMemberId() &&
                sceneModel.sceneId != memberObject.homeSceneId;

        } else if (sceneModel.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
            _homeButton.toolTip = Msgs.EDITING.get("b.make_group_home");
            _homeButton.enabled = false;

            if (memberObject.isGroupManager(sceneModel.ownerId)) {
                var svc :MemberService = 
                    _controller.ctx.getClient().requireService(MemberService) as MemberService;
                svc.getGroupHomeSceneId(_controller.ctx.getClient(), sceneModel.ownerId,
                    new ResultAdapter(
                        // failed function
                        function (cause :String) :void {
                            _controller.ctx.displayFeedback(MsoyCodes.EDITING_MSGS, cause);
                        },
                        // processed function
                        function (result :Object) :void {
                            _homeButton.enabled = sceneModel.sceneId != (result as int);
                        }
                    ));
            }

        } else {
            log.warning("unrecognized room ownership type [" + sceneModel.ownerType + "]");
            _homeButton.enabled = false;
        }
        box.addChild(_homeButton);

        var spacer :Spacer = new Spacer();
        spacer.percentWidth = 100;
        box.addChild(spacer);
        
        _buttonbar = new ToggleButtonBar();
        _buttonbar.styleName = "roomEditAccessButtons";
        box.addChild(_buttonbar);

        addChild(makePanelButtons());
    }

    // @Override from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        _buttonbar.addEventListener(ItemClickEvent.ITEM_CLICK, applyHandler);
        _name.addEventListener(Event.CHANGE, changedHandler);
        _name.addEventListener(FlexEvent.ENTER, applyHandler);
    }

    // @Override from BasePanel
    override protected function applyChanges () :void
    {
        super.applyChanges();

        var model :MsoySceneModel = _controller.scene.getSceneModel() as MsoySceneModel;
        if (_name.text != model.name || _buttonbar.selectedIndex != model.accessControl) {
            // configure an update
            var newscene :MsoyScene = _controller.scene.clone() as MsoyScene;
            var newmodel :MsoySceneModel = newscene.getSceneModel() as MsoySceneModel;
            newmodel.name = (isRoomNameValid() ? _name.text : model.name);
            newmodel.accessControl = _buttonbar.selectedIndex;
            _controller.updateScene(_controller.scene, newscene);
        }
    }

    // @Override from BasePanel
    override protected function changedHandler (event :Event) :void
    {
        // note: no call to super, this is a complete replacement

        // only display apply/cancel buttons if the name field is valid
        if (isRoomNameValid()) {
            setChanged(true);
        }
    }

    protected function isRoomNameValid () :Boolean
    {
        return _name != null &&
            _name.text.length > 0 &&
            _name.text.length < 255;
    }

    protected function updateAccessButtons () :void
    {
        if (_controller.scene == null) {
            return; // nothing to do
        }

        var model :MsoySceneModel = _controller.scene.getSceneModel() as MsoySceneModel;
        if (_buttonbar.dataProvider == null) {
            var defs :Array = new Array();
            for each (var ii :int in [ MsoySceneModel.ACCESS_EVERYONE,
                                       MsoySceneModel.ACCESS_OWNER_AND_FRIENDS,
                                       MsoySceneModel.ACCESS_OWNER_ONLY ]) {
                var tip :String = Msgs.EDITING.get("m.access_" + model.ownerType + "_" + ii);
                defs.push({ id: ii, icon: ICONS[ii], toolTip: tip });
            }
            _buttonbar.dataProvider = defs;
        }
        _buttonbar.selectedIndex = model.accessControl;
    }

    private const log :Log = Log.getLog(this);

    protected var _name :TextInput;
    protected var _buttonbar :ToggleButtonBar;
    protected var _homeButton :CommandButton;

    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/button_access_everyone.png")]
    protected static const ICON_EVERYONE :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/button_access_owner_and_friends.png")]
    protected static const ICON_OWNER_FRIENDS :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/button_access_owner_only.png")]
    protected static const ICON_OWNER :Class;
    protected static const ICONS :Array = [ ICON_EVERYONE, ICON_OWNER_FRIENDS, ICON_OWNER ];
}
}
