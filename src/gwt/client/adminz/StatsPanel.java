//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.gwt.ui.FluentTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.StatsModel;

import client.ui.MsoyUI;
import client.util.InfoCallback;

/**
 * Displays server statistics.
 */
public class StatsPanel extends FlowPanel
{
    public StatsPanel ()
    {
        addStyleName("statsPanel");

        _controls = new FlowPanel();
        _controls.setStyleName("Controls");
        _controls.add(MsoyUI.createLabel(_msgs.statsType(), "inline"));
        _controls.add(_types = new ListBox());
        for (StatsModel.Type type : StatsModel.Type.values()) {
            _types.addItem(type.toString());
        }
        _types.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent sender) {
                refresh();
            }
        });
        _controls.add(new Button(_msgs.statsRefresh(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                refresh();
            }
        }));
        add(_controls);
    }

    protected void refresh ()
    {
        String tstr = _types.getItemText(_types.getSelectedIndex());
        final StatsModel.Type type = Enum.valueOf(StatsModel.Type.class, tstr);
        _adminsvc.getStatsModel(type, new InfoCallback<StatsModel>() {
            public void onSuccess (StatsModel model) {
                displayReport(type, model);
            }
        });
    }

    protected void displayReport (StatsModel.Type type, StatsModel model)
    {
        clear();
        add(_controls);

        FluentTable table = new FluentTable(5, 0);
        FluentTable.Cell cell = table.add().setText(model.getTitleHeader());
        for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
            cell.right().setText(model.getColumnHeader(cc)).alignCenter();
        }
        for (int rr = 0, lr = model.getRows(); rr < lr; rr++) {
            cell = table.add().setText(model.getRowTitle(rr));
            for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
                cell.right().setText(""+model.getCell(rr, cc).value(model)).alignRight();
            }
        }
        add(table);
    }

    protected FlowPanel _controls;
    protected ListBox _types;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);

    protected static final int DATA_WIDTH = 50;
}
