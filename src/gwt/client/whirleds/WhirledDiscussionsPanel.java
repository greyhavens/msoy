//
// $Id: WhirledMembersPanel.java 9296 2008-05-28 14:51:30Z mdb $

package client.whirleds;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.item.data.all.MediaDesc;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;

/**
 * Displays the members of a particular Whirled. Allows managers to manage ranks and membership.
 */
public class WhirledDiscussionsPanel extends FlowPanel
{
    public WhirledDiscussionsPanel (GroupDetail detail)
    {
        setStyleName("WhirledDiscussionPanel");
        _detail = detail;
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _loaded || _detail == null) {
            return;
        }
        _loaded = true;
        
        Anchor rss = new Anchor("/rss/" + _detail.group.groupId, "", "_blank");
        rss.setHTML("<img src='/images/whirled/thread_rss.png'/>");
        rss.setStyleName("RSS");
        add(rss);
        
        // there are no threads, print a message
        if (_detail.threads.size() == 0) {
            HTML noThreads = new HTML(CWhirleds.msgs.discussionNoThreads());
            noThreads.setStyleName("NoThreads");
            add(noThreads);
            return;
        }
        
        // add a ThreadWidget and divider for each thread
        for (int i = 0; i < _detail.threads.size(); i++) {
            ForumThread thread = (ForumThread)_detail.threads.get(i);
            add(new ThreadWidget(thread));
            if (_detail.threads.size() > i+1) {
                SimplePanel divider = new SimplePanel();
                divider.setStyleName("Divider");
                add(divider);
            }
        }
    }
    
    /**
     * Displays the original post of a single thread.
     */
    protected class ThreadWidget extends FlowPanel
    {
        public ThreadWidget (final ForumThread thread) {
            setStyleName("Thread");
            
            Label date = new Label(DATE_FORMAT.format(thread.mostRecentPostTime));
            date.setStyleName("Date");
            add(date);
            
            Widget subject = Application.createLink(
                thread.subject, Page.WHIRLEDS, Args.compose("t", thread.threadId));
            subject.setStyleName("Subject");
            add(subject);
            
            HTML text = new HTML(thread.firstPost.message);
            text.setStyleName("Text");
            add(text);

            ClickListener posterClick = Application.createLinkListener(
                Page.PEOPLE, "" + thread.firstPost.poster.name.getMemberId());
            Widget posterIcon = MediaUtil.createMediaView(
                thread.firstPost.poster.photo, MediaDesc.HALF_THUMBNAIL_SIZE, posterClick);
            posterIcon.setStyleName("PostedByIcon");
            add(posterIcon);
            
            // posted by <a href="#people-{ID}">{NAME}</a> at {TIME}
            HTML posterName = new HTML(CWhirleds.msgs.discussionPostedBy(
                ""+thread.firstPost.poster.name.getMemberId(),
                thread.firstPost.poster.name.toString(), 
                TIME_FORMAT.format(thread.firstPost.created)));
            posterName.setStyleName("PostedBy");
            add(posterName);
            
            final String repliesText;
            if (thread.posts == 2) {
                // one reply - singular
                repliesText = CWhirleds.msgs.discussionReply("1");
            }
            else {
                // 0 or 2+ replies
                repliesText = CWhirleds.msgs.discussionReplies("" + (thread.posts-1));
            }
            Widget replies = Application.createLink(
                repliesText, Page.WHIRLEDS, Args.compose("t", thread.threadId));
            replies.setStyleName("Replies");
            add(replies);
        }
    }

    /** Remembers once data is loaded for the first time to prevent reloading */
    protected boolean _loaded = false;

    /** Infoes about the group we're in for constructing links etc */
    protected GroupDetail _detail;
    
    /** Used to format the most recent post date. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy");

    /** Used to format the most recent post date. */
    protected static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm aa");
}
