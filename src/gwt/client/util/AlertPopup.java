// 
// $Id$

package client.util;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;

/**
 * A class that will throw an alert up in front of the user, with an "ok" button at the bottom
 */
public class AlertPopup extends BorderedPopup
{
    public AlertPopup (String prompt)
    {
        this(prompt, "Ok");
    }

    public AlertPopup (String prompt, String button) 
    {
        _prompt = prompt;
        _button = button;
    }

    /**
     * This method throws up the alert text, and calls the onButton() when the button is pressed
     */
    public void alert ()
    {
        VerticalPanel content = new VerticalPanel();
        // follow the stylings of PromptPopup for now
        content.setStyleName("promptPopup");

        Label headerLabel = new Label(_prompt);
        headerLabel.setStyleName("Header");
        content.add(headerLabel);
        content.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        Button okButton = new Button(_button);
        okButton.addClickListener(new ClickListener () {
            public void onClick (Widget sender) {
                onButton();
                hide();
            }
        });
        content.add(okButton);
        setWidget(content);
        show();
    }

    /**
     * Called when the user presses the button.
     */
    public void onButton () 
    {
    }

    protected String _prompt;
    protected String _button;
}
