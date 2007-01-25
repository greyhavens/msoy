//
// $Id$

package client.editem;


import com.google.gwt.user.client.ui.FlexTable;
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
    public Item createBlankItem ()
    {
        Game game = new Game();
        game.config = "";
        return game;
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        // configure the main uploader first
        tabs.add(createMainUploader(CEditem.emsgs.gameMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                // TODO: validate media type
                _game.gameMedia = desc;
                return null;
            }
        }), CEditem.emsgs.gameMainTab());

        String title = CEditem.emsgs.gameTableTitle();
        tabs.add(_tableUploader = createUploader(TABLE_ID, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return CEditem.emsgs.errTableNotImage();
                }
                _game.tableMedia = desc;
                return null;
            }
        }), CEditem.emsgs.gameTableTab());

        FlexTable bits = new FlexTable();
        tabs.add(bits, CEditem.emsgs.gameConfigTab());

        // TODO: it'd be nice to force-format this text field for integers, or something.
        int row = 0;
        bits.setText(row, 0, CEditem.emsgs.gameMinPlayers());
        bits.setWidget(row++, 1, bind(_minPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.minPlayers = asShort(text);
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.gameMaxPlayers());
        bits.setWidget(row++, 1, bind(_maxPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.maxPlayers = asShort(text);
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.gameDesiredPlayers());
        bits.setWidget(row++, 1, bind(_desiredPlayers = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _game.desiredPlayers = asShort(text);
            }
        }));

        bits.setText(row++, 0, CEditem.emsgs.gameDefinition());
        bits.setWidget(row, 0, bind(_gamedef = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                _game.config = text;
            }
        }));
        bits.getFlexCellFormatter().setColSpan(row++, 0, 2);
        _gamedef.setCharacterWidth(80);
        _gamedef.setVisibleLines(5);

        super.createInterface(contents, tabs);
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
