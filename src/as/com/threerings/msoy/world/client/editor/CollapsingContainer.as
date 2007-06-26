//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.core.Container;

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
        _contents = new Canvas();
        this.percentWidth = 100;
    }

    public function setContents (child :Container) :void
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
        var labelrow :HBox = new HBox();
        labelrow.styleName = "roomEditLabelContainer";
        labelrow.setStyle("verticalAlign", "middle");
        labelrow.percentWidth = 100;
        addChild(labelrow); 
        
        var label :Label = new Label();
        label.styleName = "roomEditLabel";
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
        
        addChild(_contents);
        toggleContents(false);
    }

    /** Shows or hides the content box. */
    protected function toggleContents (enabled :Boolean) :void
    {
        _contents.visible = _contents.includeInLayout = enabled;
    }

    protected var _labeltext :String;
    protected var _expanded :Boolean;
    protected var _contents :Container;
}
}
