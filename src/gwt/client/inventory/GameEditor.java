//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.MediaDesc;

import client.MsoyEntryPoint;

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
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        // configure the main uploader first
        configureMainUploader("Upload your game.", new MediaUpdater() {
            public void updateMedia (byte[] hash, byte mimeType) {
                _game.gameMedia = new MediaDesc(hash, mimeType);
            }
        });

        super.createEditorInterface();

        addRow("Name", _name = new TextBox());
        bind(_name, new Binder() {
            public void textUpdated (String text) {
                _game.name = text;
            }
        });

        // TODO: it'd be nice to force-format this text field for integers,
        // or something.
        addRow("Minimum players", _minPlayers = new TextBox());
        bind(_minPlayers, new Binder() {
            public void textUpdated (String text) {
                _game.minPlayers = asShort(text);
            }
        });

        addRow("Maximum players", _maxPlayers = new TextBox());
        bind(_maxPlayers, new Binder() {
            public void textUpdated (String text) {
                _game.maxPlayers = asShort(text);
            }
        });

        addRow("Desired players", _desiredPlayers = new TextBox());
        bind(_desiredPlayers, new Binder() {
            public void textUpdated (String text) {
                _game.desiredPlayers = asShort(text);
            }
        });

        addRow("Game definition", _gamedef = new TextArea());
        bind(_gamedef, new Binder() {
            public void textUpdated (String text) {
                _game.config = text;
            }
        });
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Game();
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
}
