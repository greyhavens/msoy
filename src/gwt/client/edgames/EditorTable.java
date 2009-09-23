//
// $Id$

package client.edgames;

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
import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;

/**
 * A base class for game editor panels.
 */
public class EditorTable extends SmartTable
{
    public EditorTable ()
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
            row = addWidget(bits, 1);
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
        addWidget(WidgetUtil.makeShim(10, 10), 2);
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

    protected class MediaBox extends EditorUtil.MediaBox
    {
        public MediaBox (int size, String mediaId, MediaDesc media) {
            super(size, mediaId, media);
        }
        @Override protected void mediaModified () {
            EditorTable.this.mediaModified();
        }
    }

    protected class CodeBox extends EditorUtil.CodeBox
    {
        public CodeBox (String emptyMessgae, String mediaId, MediaDesc media) {
            super(emptyMessgae, mediaId, media);
        }
        @Override protected void mediaModified () {
            EditorTable.this.mediaModified();
        }
    }

    protected Button _save;
    protected CheckBox _confirm;
    protected List<Command> _binders = new ArrayList<Command>();

    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final EditGameServiceAsync _gamesvc = GWT.create(EditGameService.class);
}
