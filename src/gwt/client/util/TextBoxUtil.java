//
// $Id$

package client.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

/**
 * {@link TextBox} and {@link TextArea} related utility methods.
 */
public class TextBoxUtil
{
    /**
     * Registers the supplied command to be called when the contents of the supplied text box
     * change due to typing.
     *
     * <p> You can't just add a KeyboardListener because onKeyPress is called *before* the key in
     * question is committed to the text box. You might think that ChangeListener was your awesome
     * friend in this case, but no, that's only triggered when someone presses return in the text
     * box. Someone needs to be punched for all of this bullshit. If anyone finds out who, please
     * let MDB know.
     */
    public static void addTypingListener (TextBoxBase box, final Command onUpdate)
    {
        box.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char charCode, int modifiers) {
                DeferredCommand.addCommand(onUpdate);
            }
        });
    }
}
