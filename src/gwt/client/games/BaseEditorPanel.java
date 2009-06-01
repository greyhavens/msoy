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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
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
        return addRow(name, null, widget, binder);
    }

    protected int addRow (String name, String tip, Widget widget, Command binder)
    {
        int row;
        if (tip == null) {
            row = addText(name, 1, "nowrapLabel");
        } else {
            FlowPanel bits = MsoyUI.createFlowPanel("Label");
            bits.add(MsoyUI.createLabel(name, "nowrapLabel"));
            bits.add(MsoyUI.createLabel(tip, "tipLabel"));
            row = addWidget(bits, 1, null);
        }
        getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        setWidget(row, 1, widget);
        getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
        if (binder != null) {
            _binders.add(binder);
        }
        return row;
    }

    protected int addTip (String text)
    {
        int row = getRowCount();
        setWidget(row, 1, MsoyUI.createHTML(text, null), 1, "tipLabel");
        return row;
    }

    protected void addSpacer ()
    {
        addWidget(WidgetUtil.makeShim(10, 10), 2, null);
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

    protected String checkName (String name)
    {
        if (name.length() == 0 || name.length() > GameInfo.MAX_NAME_LENGTH) {
            throw new ConfigException(_msgs.errInvalidName(""+GameInfo.MAX_NAME_LENGTH));
        }
        return name;
    }

    protected MediaDesc checkImageMedia (String type, MediaDesc desc)
    {
        if (desc != null && !desc.isImage()) {
            throw new ConfigException(_msgs.errInvalidImage(type));
        }
        return desc;
    }

    protected MediaDesc requireImageMedia (String type, MediaDesc desc)
    {
        if (desc == null || !desc.isImage()) {
            throw new ConfigException(_msgs.errInvalidImage(type));
        }
        return desc;
    }

    protected MediaDesc checkClientMedia (MediaDesc desc)
    {
        if (desc == null || !desc.isSWF()) {
            throw new ConfigException(_msgs.errInvalidClientCode());
        }
        return desc;
    }

    protected MediaDesc checkServerMedia (MediaDesc desc)
    {
        if (desc == null || desc.mimeType != MediaDesc.COMPILED_ACTIONSCRIPT_LIBRARY) {
            throw new ConfigException(_msgs.errInvalidServerCode());
        }
        return desc;
    }

    protected class MediaBox extends SmartTable
        implements MediaUploader.Listener
    {
        public MediaBox (int size, String mediaId, MediaDesc media) {
            super("mediaBox", 0, 0);
            _size = size;
            setMedia(media);
            setWidget(1, 0, new MediaUploader(mediaId, this));
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

    protected class CodeBox extends SmartTable
        implements MediaUploader.Listener
    {
        public CodeBox (String emptyMessgae, String mediaId, MediaDesc media) {
            super("codeBox", 0, 0);
            _emptyMessage = emptyMessgae;
            setMedia(media);
            setWidget(1, 0, new MediaUploader(mediaId, this));
        }

        public void setMedia (MediaDesc media) {
            _media = media;
            setText(0, 0, (media == null) ? _emptyMessage : media.toString(), 1, "Code");
        }

        public MediaDesc getMedia () {
            return _media;
        }

        // from MediaUploader.Listener
        public void mediaUploaded (String name, MediaDesc desc, int width, int height) {
            setMedia(desc);
            mediaModified();
        }

        protected String _emptyMessage;
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
