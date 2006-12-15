//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Game} digital items.
 */
public class GameEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _game = (Game)item;
        _minPlayers.setText("" + _game.minPlayers);
        _maxPlayers.setText("" + _game.maxPlayers);
        _desiredPlayers.setText("" + _game.desiredPlayers);
        _gamedef.setText(_game.config);
        _tableUploader.setMedia(_game.getTableMedia());
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        // configure the main uploader first
        String title = "Main Game media";
        tabs.add(createMainUploader(title, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _game.gameMedia = desc;
                return null;
            }
        }), "Game Media");

        title = "Game Lobby Table background image";
        tabs.add(_tableUploader = new MediaUploader(TABLE_ID, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return "Table images must be an image type.";
                }
                _game.tableMedia = desc;
                recenter(true);
                return null;
            }
        }), "Lobby Table Background");

        super.createInterface(contents, tabs);

        // TODO: it'd be nice to force-format this text field for integers, or something.
        contents.add(createRow("Minimum players", bind(_minPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.minPlayers = asShort(text);
            }
        })));

        contents.add(createRow("Maximum players", bind(_maxPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.maxPlayers = asShort(text);
            }
        })));

        contents.add(createRow("Desired players",
                               bind(_desiredPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.desiredPlayers = asShort(text);
            }
        })));

        contents.add(createRow("Game definition", bind(_gamedef = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                _game.config = text;
            }
        })));
        _gamedef.setCharacterWidth(40);
        _gamedef.setVisibleLines(5);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Game();
    }

    // @Override from ItemEditor
    protected MediaUploader getUploader (String id)
    {
        if (TABLE_ID.equals(id)) {
            return _tableUploader;
        } else {
            return super.getUploader(id);
        }
    }

    // mr. utility
    protected static short asShort (String s)
    {
        try {
            return (short) Integer.parseInt(s);
        } catch (Exception e) {
            return (short) 0;
        }
    }

    protected Game _game;

    protected TextBox _minPlayers, _maxPlayers, _desiredPlayers;
    protected TextArea _gamedef;

    protected MediaUploader _tableUploader;

    protected static final String TABLE_ID = "table";
}
