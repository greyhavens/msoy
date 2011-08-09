//
// $Id$

package client.me;

import java.util.List;

import com.google.common.collect.Lists;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.web.gwt.Activity;

import client.comment.CommentsPanel;
import client.person.FeedUtil;
import client.person.FeedMessagePanel;
import client.shell.CShell;
import client.ui.MsoyUI;

import com.threerings.gwt.util.Console;

public class StreamPanel extends CommentsPanel
{
    public StreamPanel (ExpanderResult<Activity> stream)
    {
        super(CommentType.PROFILE_WALL, CShell.getMemberId(), true);
        addStyleName("Stream");

        Console.log("StreamPanel", "stream", stream);

        _preloaded = stream;
    }

    @Override
    protected Widget createElement (Activity activity)
    {
        if (activity instanceof FeedMessage) {
            FeedMessage message = (FeedMessage) activity;
            return new FeedMessagePanel(message, true, "your");
        }
        return super.createElement(activity);
    }

    @Override
    protected void fetchElements (AsyncCallback<ExpanderResult<Activity>> callback)
    {
        Console.log("fetchElements");

        if (_preloaded != null) {
            callback.onSuccess(_preloaded);
            _preloaded = null;
        } else {
            _mesvc.loadStream(_earliest, 10, callback);
        }

        Console.log("okbye");
    }

    @Override
    public void addElements (List<Activity> result, boolean append)
    {
        List<Activity> aggregated = Lists.newArrayList();
        _earliest = FeedUtil.aggregate(result, aggregated);
        super.addElements(aggregated, append);
    }

    protected static final MeServiceAsync _mesvc = GWT.create(MeService.class);

    protected ExpanderResult<Activity> _preloaded;
}
