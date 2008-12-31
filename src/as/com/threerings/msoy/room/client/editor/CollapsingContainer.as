//
// $Id$

package com.threerings.msoy.room.client.editor {

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.core.Container;
import mx.core.UIComponent;

import com.threerings.flex.CommandButton;

/**
 * Container whose lower part can be expanded or collapsed.
 * Please note: use setContents() to add a container that will be expanded or collapsed.
 * Only one container can be displayed; adding a new contents container will remove the old one.
 */
public class CollapsingContainer extends VBox
{
    public function CollapsingContainer (label :String)
    {
        _labeltext = label;
        _contents = new HBox();
        _contents.styleName = "roomEditCollapsingContents";
        this.percentWidth = 100;
    }

    public function setContents (child :UIComponent) :void
    {
        if (_contents.numChildren != 0) {
            _contents.removeAllChildren();
        }
        if (child != null) {
            _contents.addChild(child);
        }
    }

    // @Override from VBox
    override protected function createChildren () :void
    {
        super.createChildren();

        this.styleName = "roomEditCollapsingContainer";

        var labelrow :HBox = new HBox();
        labelrow.setStyle("verticalAlign", "middle");
        labelrow.percentWidth = 100;
        addChild(labelrow);

        var label :Label = new Label();
        label.text = _labeltext;
        labelrow.addChild(label);

        var spacer :Spacer = new Spacer();
        spacer.percentWidth = 100;
        labelrow.addChild(spacer);

        var expand :CommandButton = new CommandButton();
        expand.styleName = "roomEditButtonExpand";
        expand.toggle = true;
        expand.width = expand.height = 9;
        expand.setCallback(toggleContents);
        labelrow.addChild(expand);

        toggleContents(false);
    }

    /** Shows or hides the content box. */
    protected function toggleContents (shouldDisplay :Boolean) :void
    {
        var currentlyDisplayed :Boolean = this.contains(_contents);

        if (shouldDisplay && ! currentlyDisplayed) {
            addChild(_contents);
        } else if (! shouldDisplay && currentlyDisplayed) {
            removeChild(_contents);
        }
    }

    protected var _labeltext :String;
    protected var _expanded :Boolean;
    protected var _contents :Container;
}
}
