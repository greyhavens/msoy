//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import client.shell.ShellMessages;

/**
 * A class that will prompt the user, and take action on affirmative or negative response. A
 * command can be provided for the affirmative action, or {@link #onAffirmative} and {@link
 * #onNegative} can be overridden. PromptPopup implements {@link ClickHandler} and {@link Command}
 * so you can have your popup directly {@link #prompt} when the target is clicked.
 */
public class PromptPopup extends BorderedPopup
    implements ClickHandler, Command
{
    /**
     * Create a PromptPopup that uses "Yes" and "No" for the buttons.
     */
    public PromptPopup (String prompt, Command onAffirmative)
    {
        this(prompt, _cmsgs.yes(), _cmsgs.no(), onAffirmative);
    }

    /**
     * @param prompt the string with which to prompt the user. This string cannot contain HTML.
     * @param affirmative the text to use on the "true" button.
     * @param negative the text to use on the "false" button.
     * @param onAffirmative the command to execute if the user clicks the affirmative button.
     */
    public PromptPopup (String prompt, String affirmative, String negative, Command onAffirmative)
    {
        super(false);
        _prompt = prompt;
        _affirmative = affirmative;
        _negative = negative;
        _onAffirmative = onAffirmative;
    }

    /**
     * Configures a context string that will be shown below the prompt message. Returns this
     * instance for easy chaining.
     */
    public PromptPopup setContext (String context)
    {
        _context = context;
        return this;
    }

    /**
     * Prompt the user, call onAffirmative or onNegative depending on user's input.
     */
    public void prompt ()
    {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("promptPopup");

        Label headerLabel = new Label(_cmsgs.promptTitle());
        headerLabel.setStyleName("Header");
        content.add(headerLabel);
        content.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

        content.add(MsoyUI.createLabel(_prompt, "Content"));
        if (_context != null) {
            content.add(MsoyUI.createLabel(_context, "Content"));
        }

        Button noButton = new Button(_negative);
        final Button yesButton = new Button(_affirmative);
        ClickHandler listener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (event.getSource() == yesButton) {
                    onAffirmative();
                } else {
                    onNegative();
                }
                hide();
            }
        };
        noButton.addClickHandler(listener);
        yesButton.addClickHandler(listener);
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(10);
        buttons.add(noButton);
        buttons.add(yesButton);
        content.add(buttons);

        setWidget(content);
        show();
    }

    /**
     * Called if the user selects the affirmative option.
     */
    public void onAffirmative ()
    {
        if (_onAffirmative != null) {
            _onAffirmative.execute();
        }
    }

    /**
     * Called if the user selects the negative option.
     */
    public void onNegative ()
    {
        // default to doing nothing, just go away
    }

    // from interface ClickHandler
    public void onClick (ClickEvent event)
    {
        prompt();
    }

    // from interface Command
    public void execute ()
    {
        prompt();
    }

    protected String _prompt, _context;
    protected String _affirmative;
    protected String _negative;
    protected Command _onAffirmative;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
