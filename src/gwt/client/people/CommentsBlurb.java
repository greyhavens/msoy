//
// $Id$

package client.people;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Activity;

import client.comment.CommentsPanel;
import client.person.FeedMessagePanel;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.util.MsoyPagedServiceDataModel;

/**
 * Displays a comment wall on a member's profile.
 */
public class CommentsBlurb extends Blurb
{
    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        setHeader(_msgs.commentsTitle(), new Image("/images/me/icon_social.png"));
        setContent(_wall = new WallPanel(pdata.name.getId()));
    }

    protected class WallPanel extends CommentsPanel
    {
        public WallPanel (int memberId) {
            super(CommentType.PROFILE_WALL, memberId, true);
            addStyleName("Wall");
            removeStyleName("dottedGrid");
            setVisible(true); // trigger immediate loading of our model
        }

        @Override
        protected Widget createElement (Activity activity)
        {
            if (activity instanceof FeedMessage) {
                FeedMessage message = (FeedMessage) activity;
                String possessive = (_name.getId() == CShell.getMemberId()) ?
                    _msgs.your() : _msgs.their(_name.toString());
                return new FeedMessagePanel(message, true, possessive);
            }
            return super.createElement(activity);
        }

        @Override
        protected void fetchElements (AsyncCallback<ExpanderResult<Activity>> callback)
        {
            _profilesvc.loadActivity(_name.getId(), _earliest, 10, callback);
        }

        @Override
        public void addElements (List<Activity> result)
        {
            result = Lists.newArrayList(result);

            Collections.sort(result, MOST_RECENT_FIRST);

            List<Activity> activities = Lists.newArrayList();
            List<FeedMessage> messages = Lists.newArrayList();

            // Aggregate continuous sections of feed messages
            for (Activity activity : result) {
                if (activity instanceof FeedMessage) {
                    messages.add((FeedMessage) activity);
                    _earliest = Math.min(_earliest, activity.startedAt());
                } else {
                    aggregate(activities, messages);
                    activities.add(activity);
                }
            }
            aggregate(activities, messages);

            super.addElements(activities);
        }

        protected void aggregate (List<Activity> activities, List<FeedMessage> messages)
        {
            if (!messages.isEmpty()) {
                activities.addAll(FeedMessageAggregator.aggregate(messages, false));
                messages.clear();
            }
        }
    }

    protected static final Comparator<Activity> MOST_RECENT_FIRST = new Comparator<Activity>() {
        public int compare (Activity a1, Activity a2) {
            return Longs.compare(a2.startedAt(), a1.startedAt());
        }
    };

    protected WallPanel _wall;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ProfileServiceAsync _profilesvc = GWT.create(ProfileService.class);
}
