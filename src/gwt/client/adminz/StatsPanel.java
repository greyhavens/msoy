//
// $Id$

package client.adminz;

import java.util.Arrays;
import java.util.Comparator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.widgetideas.table.client.FixedWidthFlexTable;
import com.google.gwt.widgetideas.table.client.FixedWidthGrid;
import com.google.gwt.widgetideas.table.client.ScrollTable;
import com.google.gwt.widgetideas.table.client.SortableGrid;
import com.google.gwt.widgetideas.table.client.TableModel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.StatsModel;

import client.shell.Frame;
import client.ui.MsoyUI;
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
        _controls.setStyleName("Controls");
        _controls.add(MsoyUI.createLabel(_msgs.statsType(), "inline"));
        _controls.add(_types = new ListBox());
        for (StatsModel.Type type : StatsModel.Type.values()) {
            _types.addItem(type.toString());
        }
        _types.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                refresh();
            }
        });
        _controls.add(new Button(_msgs.statsRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        }));
        add(_controls);
    }

    protected void refresh ()
    {
        String tstr = _types.getItemText(_types.getSelectedIndex());
        final StatsModel.Type type = Enum.valueOf(StatsModel.Type.class, tstr);
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

        // set up the header
        FixedWidthFlexTable header = new FixedWidthFlexTable();
        header.setCellSpacing(0);
        header.setText(0, 0, model.getTitleHeader());
        for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
            header.setText(0, cc+1, model.getColumnHeader(cc));
            header.getCellFormatter().setHorizontalAlignment(0, cc+1, HasAlignment.ALIGN_CENTER);
        }

        // set up the table contents
        FixedWidthGrid data = new FixedWidthGrid();
        data.setCellSpacing(0);
//         data.setColumnSorter(new StatsColumnSorter(model));
        for (int rr = 0, lr = model.getRows(); rr < lr; rr++) {
            data.setText(rr, 0, model.getRowTitle(rr));
            for (int cc = 0, lc = model.getColumns(); cc < lc; cc++) {
                data.setText(rr, cc+1, ""+model.getCell(rr, cc).value(model));
                data.getCellFormatter().setHorizontalAlignment(rr, cc+1, HasAlignment.ALIGN_RIGHT);
            }
        }

        // create our table
        ScrollTable table = new ScrollTable(data, header);
        table.setSortingEnabled(true);
        table.setResizePolicy(ScrollTable.ResizePolicy.UNCONSTRAINED);
        table.setScrollPolicy(ScrollTable.ScrollPolicy.HORIZONTAL);
        int availwid = Frame.CONTENT_WIDTH - 11 - (model.getColumns()+1)*3; // border, colgap
        int colwid = Math.min(100, Math.max(DATA_WIDTH, (availwid-200)/model.getColumns()));
        table.setColumnWidth(0, Math.max(100, availwid - (model.getColumns() * colwid)));
        for (int cc = 0; cc < model.getColumns(); cc++) {
            table.setColumnWidth(cc+1, colwid);
        }
        add(table);
    }

    protected static class StatsColumnSorter extends SortableGrid.ColumnSorter
    {
        public StatsColumnSorter (StatsModel model) {
            _model = model;
            _curIndexes = makeIndexes();
        }

        @Override // from SortableGrid.ColumnSorter
        public void onSortColumn (SortableGrid grid, TableModel.ColumnSortList sortList,
                                  SortableGrid.ColumnSorterCallback callback) {
            final int column = sortList.getPrimaryColumn()-1; // -1 accounts for row label
            final boolean ascending = sortList.isPrimaryAscending();
            Integer[] newIndexes = makeIndexes();
            Arrays.sort(newIndexes, new Comparator<Integer>() {
                public int compare (Integer r1, Integer r2) {
                    long r1v = _model.value(_curIndexes[r1], column);
                    long r2v = _model.value(_curIndexes[r2], column);
                    if (ascending) {
                        return (r1v > r2v) ? 1 : (r1v < r2v ? -1 : 0);
                    } else {
                        return (r2v > r1v) ? 1 : (r2v < r1v ? -1 : 0);
                    }
                }
            });
            int[] indexes = new int[newIndexes.length];
            for (int ii = 0; ii < _curIndexes.length; ii++) {
                indexes[ii] = newIndexes[ii];
                _curIndexes[ii] = _curIndexes[newIndexes[ii]];
            }
            callback.onSortingComplete(indexes);
        }

        protected Integer[] makeIndexes () {
            Integer[] indexes = new Integer[_model.getRows()];
            for (int ii = 0; ii < indexes.length; ii++) {
                indexes[ii] = ii;
            }
            return indexes;
        }

        protected StatsModel _model;
        protected Integer[] _curIndexes;
    }

    protected FlowPanel _controls;
    protected ListBox _types;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);

    protected static final int DATA_WIDTH = 50;
}
