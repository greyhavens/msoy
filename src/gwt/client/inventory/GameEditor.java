//
// $Id$

package client.inventory;

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
        _name.setText((_game.name == null) ? "" : _game.name);
        _minPlayers.setText("" + _game.minPlayers);
        _maxPlayers.setText("" + _game.maxPlayers);
        _desiredPlayers.setText("" + _game.desiredPlayers);
        _gamedef.setText(_game.config);
        _tableUploader.setMedia(_game.getTableMedia());
    }

    // @Override from ItemEditor
    protected void createMainInterface (VerticalPanel main)
    {
        super.createMainInterface(main);

        // configure the main uploader first
        main.add(createMainUploader("Game Media", new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _game.gameMedia = desc;
                return null;
            }
        }));

        String title = "Table image";
        main.add(_tableUploader = new MediaUploader(TABLE_ID, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return "Table images must be an image type.";
                }
                _game.tableMedia = desc;
                recenter(true);
                return null;
            }
        }));

        main.add(createRow("Name", bind(_name = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.name = text;
            }
        })));

        // TODO: it'd be nice to force-format this text field for integers, or something.
        main.add(createRow("Minimum players", bind(_minPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.minPlayers = asShort(text);
            }
        })));

        main.add(createRow("Maximum players", bind(_maxPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.maxPlayers = asShort(text);
            }
        })));

        main.add(createRow("Desired players", bind(_desiredPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.desiredPlayers = asShort(text);
            }
        })));

        main.add(createRow("Game definition", bind(_gamedef = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                _game.config = text;
            }
        })));
        _gamedef.setCharacterWidth(80);
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
    protected TextBox _name;

    protected TextBox _minPlayers, _maxPlayers, _desiredPlayers;
    protected TextArea _gamedef;

    protected MediaUploader _tableUploader;

    protected static final String TABLE_ID = "table";
}
