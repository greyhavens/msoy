//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.StyledTextPanel;

import com.threerings.msoy.web.data.GameDetail;

import client.util.MsoyUI;

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
        String instructions = (_detail.instructions == null || _detail.instructions.length() == 0) ?
            CGame.msgs.ipNoInstructions() : _detail.instructions;
        clear();
        setHorizontalAlignment(ALIGN_LEFT);
        add(new StyledTextPanel(instructions));

        // if this is the owner of the game, add an edit button below the instructions
        if (_detail.sourceItem != null && _detail.sourceItem.ownerId == CGame.getMemberId()) {
            setHorizontalAlignment(ALIGN_RIGHT);
            add(new Button("Edit", new ClickListener() {
                public void onClick (Widget source) {
                    editInstructions();
                }
            }));
        }
    }

    protected void editInstructions ()
    {
        clear();

        final TextArea editor = new TextArea();
        editor.setWidth("100%");
        editor.setVisibleLines(20);
        if (_detail.instructions != null) {
            editor.setText(_detail.instructions);
        }
        add(editor);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        buttons.add(new Button("Cancel", new ClickListener() {
            public void onClick (Widget source) {
                showInstructions();
            }
        }));
        buttons.add(new Button("Update", new ClickListener() {
            public void onClick (Widget source) {
                saveInstructions(editor.getText());
            }
        }));
        add(buttons);
    }

    protected void saveInstructions (final String instructions)
    {
        CGame.gamesvc.updateGameInstructions(CGame.ident, _detail.gameId, instructions,
                                             new AsyncCallback() {
            public void onSuccess (Object result) {
                _detail.instructions = instructions;
                showInstructions();
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CGame.serverError(caught));
            }
        });
    }

    protected GameDetail _detail;
}
