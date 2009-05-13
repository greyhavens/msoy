//
// $Id$

package client.games;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.MediaUploader;
import client.util.MediaUtil;
import client.util.ServiceUtil;

/**
 * A base class for game editor panels.
 */
public class BaseEditorPanel extends SmartTable
{
    public BaseEditorPanel ()
    {
        super("baseEditor", 0, 10);
    }

    protected int addRow (String name, Widget widget, Command binder)
    {
        int row = addText(name, 1, "nowrapLabel");
        getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        setWidget(row, 1, widget);
        _binders.add(binder);
        return row;
    }

    protected int addTip (String text)
    {
        int row = getRowCount();
        setText(row, 1, text, 1, "Tip");
        return row;
    }

    protected Button addSaveRow ()
    {
        _save = new Button(_msgs.egSave());
        _confirm = new CheckBox(_msgs.egCopyright());
        _confirm.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            public void onValueChange (ValueChangeEvent<Boolean> event) {
                _save.setEnabled(event.getValue());
            }
        });
        _confirm.setVisible(false);

        HorizontalPanel bits = new HorizontalPanel();
        bits.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        bits.add(_save);
        bits.add(WidgetUtil.makeShim(10, 10));
        bits.add(_confirm);
        setWidget(getRowCount(), 1, bits);

        return _save;
    }

    protected void mediaModified ()
    {
        _save.setEnabled(false);
        _confirm.setValue(false);
        _confirm.setVisible(true);
    }

    protected boolean bindChanges ()
    {
        try {
            for (Command binder : _binders) {
                binder.execute();
            }
            return true;
        } catch (Exception e) {
            MsoyUI.error(e.getMessage()); // TODO: reinstate Binder + target widget, use errorNear
            return false;
        }
    }

    protected class MediaBox extends SmartTable
        implements MediaUploader.Listener
    {
        public MediaBox (int size, String mediaId, MediaDesc media) {
            super("mediaBox", 0, 0);
            _size = size;
            setMedia(media);
            setWidget(0, 1, WidgetUtil.makeShim(5, 5));
            setWidget(0, 2, new MediaUploader(mediaId, this));
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_BOTTOM);
        }

        public void setMedia (MediaDesc media) {
            if (media == null) {
                setText(0, 0, "");
            } else {
                setWidget(0, 0, MediaUtil.createMediaView(_media = media, _size));
            }
        }

        public MediaDesc getMedia () {
            return _media;
        }

        // from MediaUploader.Listener
        public void mediaUploaded (String name, MediaDesc desc, int width, int height) {
            setMedia(desc);
            mediaModified();
        }

        protected int _size;
        protected MediaDesc _media;
    }

    protected static class ConfigException extends RuntimeException
    {
        public ConfigException (String message) {
            super(message);
        }
    }

    protected Button _save;
    protected CheckBox _confirm;
    protected List<Command> _binders = new ArrayList<Command>();

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
