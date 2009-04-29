//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Utility methods for widgets that allow text to be edited.
 */
public class TextBoxUtil
{
    /**
     * Registers the supplied command to be called when the contents of the supplied text box
     * change due to typing.
     *
     * <p> You can't just add a KeyPressListener because onKeyPress is called *before* the key in
     * question is committed to the text box. You might think that ChangeListener was your awesome
     * friend in this case, but no, that's only triggered when someone presses return in the text
     * box. Someone needs to be punched for all of this bullshit. If anyone finds out who, please
     * let MDB know.
     */
    public static void addTypingListener (HasKeyPressHandlers box, final Command onUpdate)
    {
        box.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress (KeyPressEvent event) {
                DeferredCommand.addCommand(onUpdate);
            }
        });
    }
}
