//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;

import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameMetrics;

import client.util.MsoyUI;

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

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _metrics != null) {
            return;
        }

        add(MsoyUI.createLabel(CGame.msgs.gmpLoading(), "Header"));
        CGame.gamesvc.loadGameMetrics(CGame.ident, _detail.gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotMetrics((GameMetrics)result);
            }
            public void onFailure (Throwable caught) {
                CGame.log("loadGameMetrics failed", caught);
                add(MsoyUI.createLabel(CGame.serverError(caught), "Header"));
            }
        });
    }

    protected void gotMetrics (GameMetrics metrics)
    {
        _metrics = metrics;
        clear();

        if (metrics.singleTotalCount > 0) {
            add(MsoyUI.createLabel("Single-player Metrics", "Header"));
            add(createTilerDisplay(metrics.singleTotalCount, metrics.singleCounts,
                                   metrics.singleMaxScore, metrics.singleScores));
        }

        if (metrics.multiTotalCount > 0) {
            add(MsoyUI.createLabel("Multiplayer Metrics", "Header"));
            add(createTilerDisplay(metrics.multiTotalCount, metrics.multiCounts,
                                   metrics.multiMaxScore, metrics.multiScores));
        }
    }

    protected FlexTable createTilerDisplay (long totalCount, int[] counts,
                                            int maxScore, float[] scores)
    {
        FlexTable table = new FlexTable();
        int row = 0;
        table.setText(row, 0, "Total count:");
        table.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        table.setText(row++, 1, ""+totalCount);

        float delta = maxScore / 100f;
        table.setText(row, 0, "Counts:");
        table.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");

        int maxCount = 0;
        for (int ii = 0; ii < counts.length; ii++) {
            maxCount = Math.max(counts[ii], maxCount);
        }
        int[] data = new int[counts.length];
        for (int ii = 0; ii < counts.length; ii++) {
            data[ii] = (counts[ii] * GRAPH_HEIGHT) / maxCount;
        }
        table.setWidget(row++, 1, createGraph(data, ""+maxCount));

        table.setText(row, 0, "Max score:");
        table.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        table.setText(row++, 1, ""+maxScore);

        table.setText(row, 0, "Scores:");
        table.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");

        data = new int[scores.length];
        for (int ii = 0; ii < counts.length; ii++) {
            data[ii] = Math.round((scores[ii] * GRAPH_HEIGHT) / maxScore);
        }
        table.setWidget(row++, 1, createGraph(data, format(maxScore)));

        return table;
    }

    protected FlexTable createGraph (int[] data, String maxY)
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

        holder.setText(0, 1, maxY);
        holder.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        holder.getFlexCellFormatter().setStyleName(0, 1, "tipLabel");

        holder.setText(1, 0, "0");
        holder.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_BOTTOM);
        holder.getFlexCellFormatter().setStyleName(1, 0, "tipLabel");

        return holder;
    }

    protected String format (float value)
    {
        return Math.round(value) + "." + Math.round((value - Math.floor(value)) * 10);
    }

    protected GameDetail _detail;
    protected GameMetrics _metrics;

    protected static final int BAR_WIDTH = 4;
    protected static final int GRAPH_HEIGHT = 200;
}
