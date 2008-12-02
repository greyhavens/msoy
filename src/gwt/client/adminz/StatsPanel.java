//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.StatsModel;

import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays server statistics.
 */
public class StatsPanel extends FlowPanel
{
    public StatsPanel ()
    {
        addStyleName("statsPanel");

        _controls = new FlowPanel();
        _controls.add(_types = new ListBox());
        for (StatsModel.Type type : StatsModel.Type.values()) {
            _types.addItem(type.toString());
        }
        _types.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                String tstr = _types.getItemText(_types.getSelectedIndex());
                selectReport(Enum.valueOf(StatsModel.Type.class, tstr));
            }
        });
        add(_controls);
    }

    protected void selectReport (final StatsModel.Type type)
    {
        _adminsvc.getStatsModel(type, new MsoyCallback<StatsModel>() {
            public void onSuccess (StatsModel model) {
                displayReport(type, model);
            }
        });
    }

    protected void displayReport (StatsModel.Type type, StatsModel model)
    {
        clear();
        add(_controls);
        SmartTable table = new SmartTable(null, 0, 10);
        table.setText(0, 0, model.getTitleHeader());
        for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
            table.setText(0, cc+1, model.getColumnHeader(cc));
        }
        for (int rr = 0, lr = model.getRows(); rr < lr; rr++) {
            table.setText(rr+1, 0, model.getRowTitle(rr));
            for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
                table.setText(rr+1, cc+1, ""+model.getCell(rr, cc).value(model));
            }
        }
        add(table);
    }

    protected FlowPanel _controls;
    protected ListBox _types;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
