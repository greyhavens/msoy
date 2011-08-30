//
// $Id$

package client.me;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.Pages;

import client.comment.CommentPanel;
import client.comment.CommentsPanel;
import client.person.FeedMessagePanel;
import client.person.FeedUtil;
import client.shell.CShell;
import client.util.Link;

public class StreamPanel extends CommentsPanel
{
    public StreamPanel (ExpanderResult<Activity> stream)
    {
        super(CommentType.PROFILE_WALL, CShell.getMemberId(), true);
        addStyleName("Stream");

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
        if (_preloaded != null) {
            callback.onSuccess(_preloaded);
            _preloaded = null;
        } else {
            _mesvc.loadStream(_earliest, MyWhirledData.STREAM_PAGE_LENGTH, callback);
        }
    }

    protected CommentPanel createCommentPanel (Comment comment)
    {
        FlowPanel authorBits = null;
        if (comment.commentor.getId() != comment.entityId
                && comment.entityId == _entityId
                && !comment.isReply()) {
            authorBits = new FlowPanel();
            authorBits.addStyleName("inline");
            authorBits.add(new InlineLabel(" " + _msgs.on() + " "));
            authorBits.add(Link.create(_msgs.yourWall(), "Author",
                Pages.PEOPLE, ""+CShell.creds.name.getId()));
        }
        return new CommentPanel(this, comment, authorBits);
    }

    @Override
    public void addElements (List<Activity> result, boolean append)
    {
        List<Activity> aggregated = Lists.newArrayList();
        _earliest = FeedUtil.aggregate(result, aggregated);
        super.addElements(aggregated, append);
    }

    @Override
    protected boolean commentsCanBeBatchDeleted ()
    {
        return false;
    }

    protected static final MeServiceAsync _mesvc = GWT.create(MeService.class);
    protected static final MeMessages _msgs = (MeMessages)GWT.create(MeMessages.class);

    protected ExpanderResult<Activity> _preloaded;
}
