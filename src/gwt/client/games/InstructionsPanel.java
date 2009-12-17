//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.richedit.MessageEditor;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.SafeHTML;
import client.util.InfoCallback;

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
            add(new Label(_msgs.ipNoInstructions()));
        } else {
            // snip off our background color if we have one
            String[] bits = decodeInstructions(_detail.instructions);
            add(new SafeHTML(bits[0]));
            if (bits[1] != null) {
                DOM.setStyleAttribute(getElement(), "color", bits[1]);
            }
            if (bits[2] != null) {
                DOM.setStyleAttribute(getElement(), "background", bits[2]);
            }
        }

        // if this is the owner of the game, add an edit button below the instructions
        if (_detail.info.isCreator(CShell.getMemberId()) || CShell.isAdmin()) {
            setHorizontalAlignment(ALIGN_RIGHT);
            add(new Button("Edit", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    editInstructions();
                }
            }));
        }
    }

    protected String[] decodeInstructions (String instructions)
    {
        // we no longer allow setting the panel text color, but we need to decode it to to make
        // sure the panel background color gets set and that old instructions continue to look
        // right
        // TODO: upgrade all instructions using this feature and remove the '{t#' code
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

        final MessageEditor.Panel editor = MessageEditor.createDefault(true);

        setHorizontalAlignment(ALIGN_LEFT);
        add(editor.asWidget());

        if (_detail.instructions != null) {
            String[] bits = decodeInstructions(_detail.instructions);
            editor.setHTML(bits[0]);
            // we no longer use the panel text color
            editor.setPanelColor(bits[2]);
        }

        setHorizontalAlignment(ALIGN_RIGHT);
        Button cancel = new Button("Cancel", new ClickHandler() {
            public void onClick (ClickEvent event) {
                showInstructions();
            }
        });
        Button update = new Button("Update", new ClickHandler() {
            public void onClick (ClickEvent event) {
                String instructions = editor.getHTML();
                String bgcolor = editor.getPanelColor();
                if (bgcolor != null && bgcolor.matches("#......")) {
                    instructions = "{bg" + bgcolor + "}" + instructions;
                }
                saveInstructions(instructions);
            }
        });
        add(MsoyUI.createButtonRow(editor.getToggler(), cancel, update));
    }

    protected void saveInstructions (final String instructions)
    {
        if (instructions.length() > GameDetail.MAX_INSTRUCTIONS_LENGTH) {
            int excess = instructions.length() - GameDetail.MAX_INSTRUCTIONS_LENGTH;
            MsoyUI.error(_msgs.ipInstructionsTooLong(""+excess));
            return;
        }
        _gamesvc.updateGameInstructions(_detail.gameId, instructions, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                _detail.instructions = instructions;
                showInstructions();
            }
        });
    }

    protected GameDetail _detail;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
