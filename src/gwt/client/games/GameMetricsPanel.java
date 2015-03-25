//
// $Id$

package client.games;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
// import com.google.gwt.widgetideas.graphics.client.Color;
// import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameDistribs;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;

/**
 * Displays the score distributions for a particular game.
 */
public class GameMetricsPanel extends VerticalPanel
{
    public GameMetricsPanel (GameDetail detail)
    {
        setStyleName("gameMetrics");
        _detail = detail;

        add(MsoyUI.createNowLoading());
        _gamesvc.loadGameMetrics(_detail.gameId, new AsyncCallback<GameDistribs>() {
            public void onSuccess (GameDistribs metrics) {
                gotMetrics(metrics);
            }
            public void onFailure (Throwable caught) {
                CShell.log("loadGameMetrics failed", caught);
                add(MsoyUI.createLabel(CShell.serverError(caught), "Header"));
            }
        });
    }

    protected void gotMetrics (GameDistribs metrics)
    {
        clear();

        int shown = 0;
        shown += addMetrics(metrics.singleDistribs, true);
        shown += addMetrics(metrics.multiDistribs, false);

        if (shown == 0) {
            add(new Label(_msgs.gmpNoMetrics()));
        }
    }

    protected int addMetrics (Map<Integer, GameDistribs.TilerSummary> metrics, boolean single)
    {
        int count = 0;
        for (Map.Entry<Integer, GameDistribs.TilerSummary> entry : metrics.entrySet()) {
            GameDistribs.TilerSummary summary = entry.getValue();
            if (summary.totalCount > 0) {
                int gameMode = entry.getKey();

                String header;
                if (gameMode == 0) {
                    header = single ? _msgs.gmpSingleHeader() : _msgs.gmpMultiHeader();

                } else {
                    header = single ? _msgs.gmpSingleHeaderWithMode("" + gameMode) :
                        _msgs.gmpMultiHeaderWithMode("" + gameMode);
                }

                add(MsoyUI.createLabel(header, "Header"));
                add(createTilerDisplay(summary.totalCount, summary.counts,
                                       summary.maxScore, summary.scores));
                add(WidgetUtil.makeShim(5, 5));
                add(createResetUI(single, gameMode));
                count ++;
            }
        }
        return count;
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

    // TODO: make tiler resetting game-mode-aware
    protected RowPanel createResetUI (final boolean single, final int gameMode)
    {
        RowPanel row = new RowPanel();
        row.add(MsoyUI.createLabel(_msgs.gmpResetHint(), "tipLabel"));
        Button reset = new Button(_msgs.gmpResetScores());
        row.add(reset);
        new ClickCallback<Void>(reset, _msgs.gmpResetConfirm()) {
            @Override protected boolean callService () {
                _gamesvc.resetGameScores(_detail.gameId, single, gameMode, this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
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

        // NOTE: GameMetricsPanel is unused, but we'll just comment out the GWTCanvas parts because
        // that's the dependency we can't be bothered to track down (or replace)

        // GWTCanvas canvas = new GWTCanvas(width, GRAPH_HEIGHT);

        // canvas.setStrokeStyle(Color.GREY);
        // canvas.strokeRect(0, 0, width-1, GRAPH_HEIGHT-1);
        // canvas.setLineWidth(0.1f);

        // int xx = 0;
        // do {
        //     canvas.moveTo(xx, 0);
        //     canvas.lineTo(xx, GRAPH_HEIGHT);
        //     canvas.stroke();
        //     xx += width/10;
        // } while (xx < width);

        // int yy = GRAPH_HEIGHT-1;
        // do {
        //     canvas.moveTo(0, yy);
        //     canvas.lineTo(width-1, yy);
        //     canvas.stroke();
        //     yy -= GRAPH_HEIGHT/5;
        // } while (yy > 0);

        // canvas.setStrokeStyle(Color.BLACK);
        // canvas.setGlobalAlpha(0.5f);
        // canvas.moveTo(0, GRAPH_HEIGHT);
        // for (int ii = 0; ii < data.length; ii++) {
        //     int height = GRAPH_HEIGHT - data[ii];
        //     canvas.lineTo(BAR_WIDTH*ii, height);
        //     canvas.lineTo(BAR_WIDTH*(ii+1), height);
        // }
        // canvas.lineTo(width-1, GRAPH_HEIGHT);
        // canvas.fill();

        // holder.setWidget(0, 0, canvas);
        // holder.getFlexCellFormatter().setRowSpan(0, 0, 2);
        // holder.getFlexCellFormatter().setColSpan(0, 0, 3);

        // // create the y-axis
        // holder.setText(0, 1, maxY);
        // holder.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        // holder.getFlexCellFormatter().setStyleName(0, 1, "tipLabel");
        // holder.setText(1, 0, "0");
        // holder.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_BOTTOM);
        // holder.getFlexCellFormatter().setStyleName(1, 0, "tipLabel");

        // // create the x-axis
        // holder.setText(2, 0, "0");
        // holder.getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_LEFT);
        // holder.setText(2, 1, xLabel);
        // holder.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_CENTER);
        // holder.setText(2, 2, maxX);
        // holder.getFlexCellFormatter().setHorizontalAlignment(2, 2, HasAlignment.ALIGN_RIGHT);

        return holder;
    }

    protected String format (float value)
    {
        return Math.round(value) + "." + Math.round((value - Math.floor(value)) * 10);
    }

    protected GameDetail _detail;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final int BAR_WIDTH = 3;
    protected static final int GRAPH_HEIGHT = 100;
}
