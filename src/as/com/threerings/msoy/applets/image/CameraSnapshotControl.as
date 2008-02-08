//
// $Id$

package com.threerings.msoy.applets.image {

import flash.media.Camera;

import mx.controls.ButtonBar;
import mx.controls.ComboBox;
import mx.controls.Label;

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
    public function CameraSnapshotControl (parent :UIComponent, returnFn :Function)
    {
        title = "Capture from Camera";
        _snapper = new CameraSnapshotter();
        _returnFn = returnFn;

        var box :VBox = new VBox();
        addChild(box);

        _wrapper = new FlexWrapper(_snapper);
        box.addChild(_wrapper);
        _wrapper.width = _snapper.width;
        _wrapper.height = _snapper.height;

        var sources :ComboBox = new ComboBox();
        sources.prompt = "Choose camera:";
        sources.dataProvider = Camera.names;
        sources.selectedItem = _snapper.getCameraName();
        sources.addEventListener(ListEvent.CHANGE, handleCameraChange);
        box.addChild(sources);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(new CommandButton("Snapshot", takeSnapshot));
        _clear = new CommandButton("Clear", clearSnapshot);
        _ok = new CommandButton("OK", close, true);
        _ok.enabled = false;
        bar.addChild(_clear);
        bar.addChild(_ok);
        bar.addChild(new CommandButton("Cancel", close, false));
        box.addChild(bar);

        PopUpManager.addPopUp(this, parent, true);
        PopUpManager.centerPopUp(this);
    }

    protected function handleCameraChange (event :ListEvent) :void
    {
        _snapper.setCameraName(ComboBox(event.target).selectedItem as String);
        _wrapper.width = _snapper.width;
        _wrapper.height = _snapper.height;
    }

    protected function takeSnapshot () :void
    {
        _snapper.takeSnapshot();
        _ok.enabled = true;
        _clear.enabled = true;
    }

    protected function clearSnapshot () :void
    {
        _snapper.clearSnapshot();
        _ok.enabled = false;
        _clear.enabled = false;
    }

    protected function close (save :Boolean) :void
    {
        if (save) {
            _returnFn(_snapper.getSnapshot());
        }
        PopUpManager.removePopUp(this);
    }

    protected var _returnFn :Function;

    protected var _snapper :CameraSnapshotter;

    protected var _wrapper :FlexWrapper;

    protected var _ok :CommandButton;

    protected var _clear :CommandButton;
}
}
