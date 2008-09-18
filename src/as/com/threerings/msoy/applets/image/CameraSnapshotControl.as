//
// $Id$

package com.threerings.msoy.applets.image {

import flash.media.Camera;

import mx.controls.ButtonBar;
import mx.controls.ComboBox;

import mx.containers.HBox;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.UIComponent;

import mx.events.ListEvent;

import mx.managers.PopUpManager;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexWrapper;

public class CameraSnapshotControl extends TitleWindow
{
    public function CameraSnapshotControl (
        ctx :ImageContext, parent :UIComponent, returnFn :Function)
    {
        _ctx = ctx;
        title = ctx.IMAGE.get("t.camera");
        _snapper = new CameraSnapshotter();
        _returnFn = returnFn;

        var box :VBox = new VBox();
        box.setStyle("horizontalAlign", "center");
        addChild(box);

        _wrapper = new FlexWrapper(_snapper);
        box.addChild(_wrapper);

        var sources :ComboBox = new ComboBox();
        sources.prompt = ctx.IMAGE.get("p.cam_src");
        sources.dataProvider = Camera.names;
        sources.selectedItem = _snapper.getCameraName();
        sources.addEventListener(ListEvent.CHANGE, handleCameraChange);

        _sizes = new ComboBox();
        _sizes.dataProvider = [
            { label: "160x120", data: [ 160, 120 ] },
            { label: "320x240", data: [ 320, 240 ] },
            { label: "640x480", data: [ 640, 480 ] } ];
        _sizes.selectedIndex = 1; // default to 320x240
        _sizes.addEventListener(ListEvent.CHANGE, updateCameraSize);

        var hbox :HBox = new HBox();
        hbox.addChild(sources);
        hbox.addChild(_sizes);
        box.addChild(hbox);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(_cancel = new CommandButton(ctx.IMAGE.get("b.cancel"), doCancel));
        bar.addChild(_snap = new CommandButton(ctx.IMAGE.get("b.snapshot"), takeSnapshot));
        _ok = new CommandButton(ctx.IMAGE.get("b.ok"), close, true);
        _ok.enabled = false;
        bar.addChild(_ok);
        box.addChild(bar);

        PopUpManager.addPopUp(this, parent, true);
        updateCameraSize();
    }

    protected function handleCameraChange (event :ListEvent) :void
    {
        _snapper.setCameraName(ComboBox(event.target).selectedItem as String);
        updateCameraSize();
    }

    protected function updateCameraSize (... ignored) :void
    {
        var obj :Object = _sizes.selectedItem;

        _snapper.setMode(int(obj.data[0]), int(obj.data[1]), 15);
        _wrapper.width = _snapper.width;
        _wrapper.height = _snapper.height;
        PopUpManager.centerPopUp(this);

        if (!_snap.enabled) {
            doCancel();
        }
    }

    protected function doCancel () :void
    {
        if (_snap.enabled) {
            close(false);

        } else {
            _snapper.clearSnapshot();
            _ok.enabled = false;
            _snap.enabled = true;
            _cancel.label = _ctx.IMAGE.get("b.cancel");
        }
    }

    protected function takeSnapshot () :void
    {
        _snapper.takeSnapshot();
        _ok.enabled = true;
        _snap.enabled = false;
        _cancel.label = _ctx.IMAGE.get("b.clear");
    }

    protected function close (save :Boolean) :void
    {
        if (save) {
            _returnFn(_snapper.getSnapshot());
        }
        PopUpManager.removePopUp(this);
    }

    protected var _ctx :ImageContext;

    protected var _returnFn :Function;

    protected var _snapper :CameraSnapshotter;

    protected var _wrapper :FlexWrapper;

    protected var _ok :CommandButton;
    protected var _snap :CommandButton;
    protected var _cancel :CommandButton;

    protected var _sizes :ComboBox;
}
}
