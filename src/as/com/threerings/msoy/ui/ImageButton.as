//
// $Id$

package com.threerings.msoy.ui {

import com.threerings.flex.CommandButton;

/**
 * A CommandButton for use with ImageButtonSkin. The main reason this is a separate class
 * is that so Flex's watered down CSS can select it based on the class name 'ImageButton' to
 * hook up the necessary skins.
 */
public class ImageButton extends CommandButton
{
    public function ImageButton (styleName :String)
    {
        this.styleName = styleName;
    }
}

}
