//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameMetrics;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.ServiceUtil;

/**
 * Displays the metrics like score distributions for a particular game.
 */
public class GameMetricsPanel extends VerticalPanel
{
    public GameMetricsPanel (GameDetail detail)
    {
        setStyleName("gameMetrics");
        _detail = detail;
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _metrics != null) {
            return;
        }

        add(MsoyUI.createLabel(_msgs.gmpLoading(), "Header"));
        _gamesvc.loadGameMetrics(_detail.gameId, new AsyncCallback<GameMetrics>() {
            public void onSuccess (GameMetrics metrics) {
                gotMetrics(metrics);
            }
            public void onFailure (Throwable caught) {
                CGames.log("loadGameMetrics failed", caught);
                add(MsoyUI.createLabel(CGames.serverError(caught), "Header"));
            }
        });
    }

    protected void gotMetrics (GameMetrics metrics)
    {
        _metrics = metrics;
        clear();

        if (metrics.singleTotalCount > 0) {
            add(MsoyUI.createLabel(_msgs.gmpSingleHeader(), "Header"));
            add(createTilerDisplay(metrics.singleTotalCount, metrics.singleCounts,
                                   metrics.singleMaxScore, metrics.singleScores));
            add(WidgetUtil.makeShim(5, 5));
            add(createResetUI(true));
        }

        if (metrics.multiTotalCount > 0) {
            add(MsoyUI.createLabel(_msgs.gmpMultiHeader(), "Header"));
            add(createTilerDisplay(metrics.multiTotalCount, metrics.multiCounts,
                                   metrics.multiMaxScore, metrics.multiScores));
            add(WidgetUtil.makeShim(5, 5));
            add(createResetUI(false));
        }

        if (metrics.singleTotalCount + metrics.multiTotalCount == 0) {
            add(new Label(_msgs.gmpNoMetrics()));
        }
    }

    protected FlexTable createTilerDisplay (long totalCount, int[] counts,
                                            int maxScore, float[] scores)
    {
        FlexTable table = new FlexTable();
        int row = 0;

        // display the hints
        table.getFlexCellFormatter().setStyleName(row, 0, "tipLabel");
        table.setText(row, 0, _msgs.gmpCountsHint());
        table.getFlexCellFormatter().setStyleName(row, 1, "tipLabel");
        table.setText(row++, 1, _msgs.gmpScoresHint());

        // display a graph of the raw counts by bucket
        int maxCount = 0;
        for (int ii = 0; ii < counts.length; ii++) {
            maxCount = Math.max(counts[ii], maxCount);
        }
        int[] data = new int[counts.length];
        for (int ii = 0; ii < counts.length; ii++) {
            data[ii] = (counts[ii] * GRAPH_HEIGHT) / maxCount;
        }
        String xLabel = _msgs.gmpCountsX(format(maxScore / 100f));
        table.setWidget(row, 0, createGraph(data, ""+maxScore, ""+maxCount, xLabel));

        // display a graph of the scores needed to achieve a particular percentile
        data = new int[scores.length];
        for (int ii = 0; ii < counts.length; ii++) {
            data[ii] = Math.round((scores[ii] * GRAPH_HEIGHT) / maxScore);
        }
        table.setWidget(row++, 1, createGraph(data, ""+(counts.length-1), format(maxScore),
                                              _msgs.gmpScoresX()));

        // display the total number of scores recorded
        table.setText(row, 0, _msgs.gmpTotalCount(""+totalCount));
        table.setText(row++, 1, _msgs.gmpMaxPercentile(""+scores[scores.length-1]));

        return table;
    }

    protected RowPanel createResetUI (final boolean single)
    {
        RowPanel row = new RowPanel();
        row.add(MsoyUI.createLabel(_msgs.gmpResetHint(), "tipLabel"));
        Button reset = new Button(_msgs.gmpResetScores());
        row.add(reset);
        new ClickCallback<Void>(reset, _msgs.gmpResetConfirm()) {
            public boolean callService () {
                _gamesvc.resetGameScores(_detail.gameId, single, this);
                return true;
            }
            public boolean gotResult (Void result) {
                MsoyUI.info(_msgs.gmpScoresReset());
                return true;
            }
        };
        return row;
    }

    protected FlexTable createGraph (int[] data, String maxX, String maxY, String xLabel)
    {
        FlexTable holder = new FlexTable();

        int width = BAR_WIDTH*data.length;
        Canvas canvas = new Canvas(width, GRAPH_HEIGHT);
        CanvasRenderingContext2D ctx = canvas.getContext2D();

        ctx.setStrokeStyle("grey");
        ctx.strokeRect(0, 0, width-1, GRAPH_HEIGHT-1);
        ctx.setLineWidth(0.1f);

        int xx = 0;
        do {
            ctx.moveTo(xx, 0);
            ctx.lineTo(xx, GRAPH_HEIGHT);
            ctx.stroke();
            xx += width/10;
        } while (xx < width);

        int yy = GRAPH_HEIGHT-1;
        do {
            ctx.moveTo(0, yy);
            ctx.lineTo(width-1, yy);
            ctx.stroke();
            yy -= GRAPH_HEIGHT/5;
        } while (yy > 0);

        ctx.setStrokeStyle("black");
        ctx.setGlobalAlpha(0.5f);
        ctx.moveTo(0, GRAPH_HEIGHT);
        for (int ii = 0; ii < data.length; ii++) {
            int height = GRAPH_HEIGHT - data[ii];
            ctx.lineTo(BAR_WIDTH*ii, height);
            ctx.lineTo(BAR_WIDTH*(ii+1), height);
        }
        ctx.lineTo(width-1, GRAPH_HEIGHT);
        ctx.fill();

        holder.setWidget(0, 0, canvas);
        holder.getFlexCellFormatter().setRowSpan(0, 0, 2);
        holder.getFlexCellFormatter().setColSpan(0, 0, 3);

        // create the y-axis
        holder.setText(0, 1, maxY);
        holder.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        holder.getFlexCellFormatter().setStyleName(0, 1, "tipLabel");
        holder.setText(1, 0, "0");
        holder.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_BOTTOM);
        holder.getFlexCellFormatter().setStyleName(1, 0, "tipLabel");

        // create the x-axis
        holder.setText(2, 0, "0");
        holder.getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_LEFT);
        holder.setText(2, 1, xLabel);
        holder.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_CENTER);
        holder.setText(2, 2, maxX);
        holder.getFlexCellFormatter().setHorizontalAlignment(2, 2, HasAlignment.ALIGN_RIGHT);

        return holder;
    }

    protected String format (float value)
    {
        return Math.round(value) + "." + Math.round((value - Math.floor(value)) * 10);
    }

    protected GameDetail _detail;
    protected GameMetrics _metrics;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);

    protected static final int BAR_WIDTH = 3;
    protected static final int GRAPH_HEIGHT = 100;
}
