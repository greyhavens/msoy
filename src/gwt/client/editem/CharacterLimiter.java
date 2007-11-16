package client.editem;

import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class CharacterLimiter extends Label
{
    public CharacterLimiter (TextArea area, final int max)
    {
        setStyleName("characterLimiter");

        area.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyUp (Widget sender, char keyCode, int modifiers) {
                limit((TextArea) sender, max);
            }
        });

        updateLabel(max);
    }

    protected void limit (TextArea area, int max)
    {
        String text = area.getText();
        if (text.length() > max) {
            text = text.substring(0, max);
            area.setText(text);
        }
        updateLabel(max - text.length());
    }

    protected void updateLabel (int count)
    {
        this.setText("(Characters remaining: " + count + ")");
    }
}
