//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.Text;

import mx.core.UIComponent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;

import com.whirled.remix.data.EditableDataPack;

public class FieldEditor extends VBox
{
    /**
     * An event dispatched upwards when a field is edited.
     */
    public static const FIELD_CHANGED :String = "remixFieldChanged";

    public function FieldEditor (ctx :RemixContext, name :String, entry :Object)
    {
        _ctx = ctx;
        _name = name;
        percentWidth = 100;

        setStyle("borderColor", 0xA1A2A2);
        setStyle("borderStyle", "solid");
        setStyle("borderSides", "left right bottom");
        setStyle("paddingLeft", 8);
        setStyle("paddingTop", 8);
        setStyle("paddingRight", 8);
        setStyle("paddingBottom", 8);

        var topBox :HBox = new HBox();
        topBox.percentWidth = 100;
        var nameArea :HBox = new HBox();
        nameArea.setStyle("horizontalAlign", "left");
        var configArea :HBox = new HBox();
        configArea.percentWidth = 100;
        configArea.setStyle("horizontalAlign", "right");

        addChild(topBox);
        topBox.addChild(nameArea);
        topBox.addChild(configArea);

        _used = new CommandCheckBox(null, handleUsedToggled);
        _used.selected = (entry.value != null);
        _used.enabled = entry.optional;
        if (_ctx.hasOptionalFields()) {
            nameArea.addChild(_used);
        }

        var lbl :Label = new Label();
        lbl.setStyle("fontWeight", "bold");
        lbl.setStyle("color", NAME_AND_VALUE_COLOR);
        lbl.text = name;
        nameArea.addChild(lbl);

        var configgers :Array = getUI(entry);
        var topConfigger :UIComponent = UIComponent(configgers[0]);
        topConfigger.setStyle("color", NAME_AND_VALUE_COLOR);
        topConfigger.maxWidth = 130;
        configArea.addChild(topConfigger);

        var botBox :HBox = new HBox();
        botBox.setStyle("verticalAlign", "bottom");
        botBox.percentWidth = 100;

        // add the description label
        var desc :Text = new Text();
        desc.selectable = false;
        desc.width = 180;
        desc.setStyle("color", 0xA1A2A2);
        if (entry.info != null) {
            desc.text = entry.info;
        }
        botBox.addChild(desc);

        var botConfigger :UIComponent = UIComponent(configgers[1]);
        botBox.addChild(botConfigger);
        addChild(botBox);

        _component = UIComponent(configgers[2]);
        _component.enabled = _used.selected;
    }

    /**
     * Returns an array of three elements, the first is put on the top row,
     * the second is put on the bottom row, and the 3rd should be one of the first two, and
     * will be assigned as the "action" configuration item.
     */
    protected function getUI (entry :Object) :Array
    {
        var label :Label = new Label();
        label.selectable = false;
        label.text = "Unknown entry of type '" + entry.type + "'.";
        return [ label, new Spacer(), label ];
    }

    protected function createEditButton (callback :Function) :CommandButton
    {
        var edit :CommandButton = new CommandButton(_ctx.REMIX.get("b.alter"), callback);
        edit.setStyle("fontSize", 9);
        edit.setStyle("fontWeight", "normal");
        return edit;
    }

    protected function handleUsedToggled (selected :Boolean) :void
    {
        _component.enabled = selected;
        updateEntry();
    }

    protected function updateEntry () :void
    {
        throw new Error("abstract");
    }

    /**
     * May be called or set as an event handler.
     */
    protected function setChanged (... ignored) :void
    {
        // dispatch it upwards
        dispatchEvent(new Event(FIELD_CHANGED, true));
    }

    /** The name of the field we're editing. */
    protected var _name :String;

    /** The component that's doing the editing. It should be able to
     * enabled/disabled. */
    protected var _component :UIComponent;

    protected var _used :CommandCheckBox;

    protected var _ctx :RemixContext;

    protected static const NAME_AND_VALUE_COLOR :uint = 0x204260;
}
}
