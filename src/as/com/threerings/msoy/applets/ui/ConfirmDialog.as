//
// $Id$

package com.threerings.msoy.applets.ui {

import mx.containers.TitleWindow;
import mx.controls.ButtonBar;

import mx.managers.PopUpManager;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.applets.AppletContext;

public class ConfirmDialog extends TitleWindow
{
    public function ConfirmDialog (
        ctx :AppletContext, title :String, message :String, confirmCallback :Function,
        cancelCallback :Function = null)
    {
        this.title = title;
        addChild(FlexUtil.createLabel(message));

        var dialog :ConfirmDialog = this;
        var closeAnd :Function = function (confirm :Boolean) :void {
            PopUpManager.removePopUp(dialog);
            if (confirm) {
                confirmCallback();
            } else {
                cancelCallback();
            }
        };

        var bar :ButtonBar = new ButtonBar();
        if (cancelCallback != null) {
            bar.addChild(new CommandButton(ctx.APPLET.get("b.cancel"), closeAnd, false));
        }
        bar.addChild(new CommandButton(ctx.APPLET.get("b.ok"), closeAnd, true));
        addChild(bar);

        PopUpManager.addPopUp(this, ctx.getApplication(), true);
        PopUpManager.centerPopUp(this);
    }
}
}
