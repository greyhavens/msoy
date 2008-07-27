//
// $Id$

package client.games;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.gwt.GameDetail;

import client.ui.MsoyUI;
import client.ui.RichTextToolbar;
import client.util.MsoyCallback;

/**
 * Displays the instructions for a game.
 */
public class InstructionsPanel extends VerticalPanel
{
    public InstructionsPanel (GameDetail detail)
    {
        setStyleName("instructionsPanel");
        _detail = detail;
        showInstructions();
    }

    protected void showInstructions ()
    {
        clear();
        DOM.setStyleAttribute(getElement(), "color", "");
        DOM.setStyleAttribute(getElement(), "background", "none");

        setHorizontalAlignment(ALIGN_LEFT);
        if (_detail.instructions == null || _detail.instructions.length() == 0) {
            add(new Label(CGames.msgs.ipNoInstructions()));
        } else {
            // snip off our background color if we have one
            String[] bits = decodeInstructions(_detail.instructions);
            add(new HTML(bits[0]));
            if (bits[1] != null) {
                DOM.setStyleAttribute(getElement(), "color", bits[1]);
            }
            if (bits[2] != null) {
                DOM.setStyleAttribute(getElement(), "background", bits[2]);
            }
        }

        // if this is the owner of the game, add an edit button below the instructions
        if (_detail.sourceItem != null && _detail.sourceItem.ownerId == CGames.getMemberId()) {
            setHorizontalAlignment(ALIGN_RIGHT);
            add(new Button("Edit", new ClickListener() {
                public void onClick (Widget source) {
                    editInstructions();
                }
            }));
        }
    }

    protected String[] decodeInstructions (String instructions)
    {
        String[] results = new String[3];
        if (instructions.length() > 9 && instructions.substring(0, 10).matches("{t#......}")) {
            results[1] = instructions.substring(2, 9);
            instructions = instructions.substring(10);
        }
        if (instructions.length() > 10 && instructions.substring(0, 11).matches("{bg#......}")) {
            results[2] = instructions.substring(3, 10);
            instructions = instructions.substring(11);
        }
        results[0] = instructions;
        return results;
    }

    protected void editInstructions ()
    {
        clear();
        DOM.setStyleAttribute(getElement(), "color", "");
        DOM.setStyleAttribute(getElement(), "background", "none");

        final RichTextArea editor = new RichTextArea();
        editor.setWidth("100%");
        editor.setHeight("300px");

        setHorizontalAlignment(ALIGN_LEFT);
        final RichTextToolbar toolbar = new RichTextToolbar(editor, true);
        add(toolbar);
        add(editor);

        if (_detail.instructions != null) {
            String[] bits = decodeInstructions(_detail.instructions);
            editor.setHTML(bits[0]);
            toolbar.setPanelColors(bits[1], bits[2]);
        }

        setHorizontalAlignment(ALIGN_RIGHT);
        Button cancel = new Button("Cancel", new ClickListener() {
            public void onClick (Widget source) {
                showInstructions();
            }
        });
        Button update = new Button("Update", new ClickListener() {
            public void onClick (Widget source) {
                String instructions = editor.getHTML();
                String bgcolor = toolbar.getBackgroundColor();
                if (bgcolor != null && bgcolor.matches("#......")) {
                    instructions = "{bg" + bgcolor + "}" + instructions;
                }
                String tcolor = toolbar.getTextColor();
                if (tcolor != null && tcolor.matches("#......")) {
                    instructions = "{t" + tcolor + "}" + instructions;
                }
                saveInstructions(instructions);
            }
        });
        add(MsoyUI.createButtonPair(cancel, update));
    }

    protected void saveInstructions (final String instructions)
    {
        if (instructions.length() > GameDetail.MAX_INSTRUCTIONS_LENGTH) {
            int excess = instructions.length() - GameDetail.MAX_INSTRUCTIONS_LENGTH;
            MsoyUI.error(CGames.msgs.ipInstructionsTooLong(""+excess));
            return;
        }
        CGames.gamesvc.updateGameInstructions(
            CGames.ident, _detail.gameId, instructions, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                _detail.instructions = instructions;
                showInstructions();
            }
        });
    }

    protected GameDetail _detail;
}
