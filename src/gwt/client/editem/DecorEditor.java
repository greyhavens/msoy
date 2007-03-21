//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Decor;

/**
 * A class for creating and editing {@link Decor} digital items.
 */
public class DecorEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _decor = (Decor)item;
        _width.setText("" + _decor.width);
        _height.setText("" + _decor.height);
        _depth.setText("" + _decor.depth);
        _horizon.setText("" + _decor.horizon);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        Decor d = new Decor();
        d.type = Decor.FIXED_IMAGE;
        d.width = 800;
        d.height = (short) Math.round(800 / ((1 + Math.sqrt(5)) / 2)); 
        d.depth = 400;
        d.horizon = 0.5f;
        return d;
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        super.createInterface(contents, tabs);

        FlexTable bits = new FlexTable();
        tabs.add(bits, CEditem.emsgs.gameConfigTab());

        // TODO: it'd be nice to force-format this text field for integers, or something.
        int row = 0;
        bits.setText(row, 0, CEditem.emsgs.decorWidth());
        bits.setWidget(row++, 1, bind(_width = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.width = asShort(text);
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorHeight());
        bits.setWidget(row++, 1, bind(_height = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.height = asShort(text);
            }
        }));

        _height.setEnabled(false); // Don't let users edit this value just yet...

        bits.setText(row, 0, CEditem.emsgs.decorDepth());
        bits.setWidget(row++, 1, bind(_depth = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.depth = asShort(text);
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorHorizon());
        bits.setWidget(row++, 1, bind(_horizon = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.horizon = asFloat(text);
            }
        }));

        /*
        // seated continuous games are disabled for now.  re-instate the commented code to
        // re-enable them as an option.
        bits.setText(row, 0, CEditem.emsgs.gameGameType());
        bits.setWidget(row++, 1, bind(_gameType = new ListBox(), new Binder() {
            public void valueChanged () {
                _game.gameType = (byte) _gameType.getSelectedIndex();
            }
        }));
        for (int ii = 0; ii < Game.GAME_TYPES; ii++) {
            _gameType.addItem(CEditem.dmsgs.getString("gameType" + ii));
        }
        _gameType.addItem(CEditem.dmsgs.getString("gameType0"));
        _gameType.addItem(CEditem.dmsgs.getString("gameType2"));

        bits.setText(row, 0, CEditem.emsgs.gameWatchable());
        _watchable = new CheckBox();
        _watchable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                if (_game != null) {
                    DeferredCommand.add(new Command() {
                        public void execute () {
                            _game.unwatchable = !_watchable.isChecked();
                            updateSubmittable();
                        }
                    });
                }
            }
        });
        bits.setWidget(row++, 1, _watchable);
        */

    }

    // mr. utility, stolen from game editor
    protected static short asShort (String s)
    {
        try {
            return (short) Integer.parseInt(s);
        } catch (Exception e) {
            return (short) 0;
        }
    }

    // these utilities are silly
    protected static float asFloat (String s)
    {
        try {
            return (float) Float.parseFloat(s);
        } catch (Exception e) {
            return (float) 0;
        }
    }

    protected Decor _decor;
    protected TextBox _width, _height, _depth, _horizon;
    protected ListBox _type;

}
