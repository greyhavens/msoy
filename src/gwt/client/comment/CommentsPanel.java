//
// $Id$

package client.comment;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.ExpanderWidget;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.gwt.CommentServiceAsync;
import com.threerings.msoy.web.gwt.Activity;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.ComplainPopup;
import client.ui.MsoyUI;
import client.ui.SafeHTML;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends ExpanderWidget<Activity>
{
    public CommentsPanel (CommentType entityType, int entityId, boolean rated)
    {
        super(_cmsgs.seeMoreComments());
        _expandLabel.addStyleName("CommentsExpand");

        addStyleName("CommentsPanel");

        _loadingPanel.addStyleName("CommentsLoading");
        _loadingPanel.add(new Image("/images/ui/dot_loader.gif"));
        // setCellAlignment(HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_MIDDLE);

        _rated = rated;
        _etype = entityType;
        _entityId = entityId;

        _expandLabel.setVisible(false);
        add(_loadingMessage = new Label(_cmsgs.loadingComments()));

        // if we're a validated member, display a button for posting a comment
        if (CShell.isRegistered()) {
            addControls();
            _commentControls.addStyleName("commentControls");
            insert(_commentControls, 0);
        }
    }

    protected void addControls ()
    {
        _post = new Button(_cmsgs.postComment(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                showPostPopup();
            }
        });
        _commentControls.add(_post);

        if (commentsCanBeBatchDeleted()) {
            _commentControls.add(WidgetUtil.makeShim(7, 1));
            Button batchButton = new Button("Delete Checked");
            _batchDelete = new DeleteClickCallback(
                batchButton, "Are you sure you want to delete these comments?");
            _commentControls.add(batchButton);
        }
    }

    public void showPostPopup ()
    {
        showPostPopup(null);
    }

    public void showPostPopup (Comment subject)
    {
        if (!MsoyUI.requireValidated()) {
            return;
        }
        _post.setEnabled(false);
        PostPanel panel = new PostPanel(subject);
        panel.show();
        panel.focus();
    }

    protected CommentPanel createCommentPanel (Comment comment)
    {
        return new CommentPanel(this, comment);
    }

    @Override
    protected Widget createElement (Activity activity)
    {
        if (activity instanceof Comment) {
            Comment comment = (Comment) activity;
            VerticalPanel panel = new VerticalPanel();
            panel.addStyleName("Thread");
            panel.add(createCommentPanel(comment));
            if (comment.hasMoreReplies) {
                panel.add(new ReplyExpander(comment));
            }
            for (Comment reply : comment.replies) {
                CommentPanel replyPanel = createCommentPanel(reply);
                _elements.put(reply, replyPanel); // so we can easily remove it later
                panel.add(replyPanel);
            }
            return panel;
        }
        throw new IllegalArgumentException("Unsupported activity type!");
    }

    @Override
    protected void fetchElements (AsyncCallback<ExpanderResult<Activity>> callback)
    {
        _commentsvc.loadComments(_etype, _entityId, _earliest, Comment.COMMENTS_PER_PAGE, callback);
    }

    @Override
    public void addElements (List<Activity> activities, boolean append)
    {
        remove(_loadingMessage);
        for (Activity activity : activities) {
            _earliest = Math.min(_earliest, activity.startedAt());
        }
        super.addElements(activities, append);
    }

    // TODO(bruno): Handle an empty wall
    // @Override // from PagedGrid
    // protected String getEmptyMessage ()
    // {
    //     return _cmsgs.noComments();
    // }

    /**
     * Returns the size of thumbnail image to use next to our comments.
     */
    protected int getThumbnailSize ()
    {
        return MediaDescSize.HALF_THUMBNAIL_SIZE;
    }

    protected boolean commentsCanBeRated ()
    {
        return _rated;
    }

    protected boolean commentsCanBeBatchDeleted ()
    {
        return CShell.isSupport();
    }

    /**
     * Returns true if the viewer can delete the supplied comment.
     */
    protected boolean canDelete (Comment comment)
    {
        return CShell.isSupport() || Comment.canDelete(
            _etype, comment.entityId, comment.commentor.getId(), CShell.getMemberId());
    }

    /**
     * Returns true if the viewer should be shown the text of the supplied comment.
     */
    protected boolean shouldDisplay (Comment comment)
    {
        return comment.currentRating >= Comment.RATED_HIDDEN;
    }

    /**
     * Returns true if the viewer should be shown the text of the supplied comment.
     */
    protected boolean shouldEmphasize (Comment comment)
    {
        return comment.currentRating >= Comment.RATED_EMPHASIZED;
    }

    /**
     * Returns true if the viewer can complain about the supplied comment.
     */
    protected boolean canComplain (Comment comment)
    {
        return CShell.isValidated() && (CShell.getMemberId() != comment.commentor.getId());
    }

    protected void postComment (Comment subject, String text)
    {
        int entityId = (subject != null) ? subject.entityId : _entityId;
        long replyTo = (subject != null) ? subject.posted : 0;
        _commentsvc.postComment(_etype, entityId, replyTo, text, new InfoCallback<Comment>() {
            public void onSuccess (Comment result) {
                postedComment(result);
            }
        });
    }

    protected Activity findActivity (long posted)
    {
        for (Activity activity : _elements.keySet()) {
            if (posted == activity.startedAt()) {
                return activity;
            }
        }
        return null;
    }

    protected void postedComment (Comment comment)
    {
        if (comment.isReply()) {
            Panel panel = (Panel) _elements.get(findActivity(comment.replyTo));
            CommentPanel replyPanel = createCommentPanel(comment);
            _elements.put(comment, replyPanel); // so we can easily remove it later
            panel.add(replyPanel);
        } else {
            addElements(Collections.singletonList((Activity) comment), false);
        }
    }

    protected void rateComment (
        Comment comment, boolean rating, InfoCallback<Integer> callback)
    {
        _commentsvc.rateComment(_etype, comment.entityId, comment.posted, rating, callback);
    }

    protected Command deleteComment (final Comment comment)
    {
        return new Command() {
            public void execute () {
                List<Long> single = Collections.singletonList(comment.posted);
                _commentsvc.deleteComments(_etype, comment.entityId, single,
                    new InfoCallback<Integer>() {
                        public void onSuccess (Integer deleted) {
                            if (deleted > 0) {
                                removeElement(comment);
                                _batchDelete.remove(comment);
                            } else {
                                MsoyUI.error(_cmsgs.commentDeletionNotAllowed());
                            }
                        }
                    });
            }
        };
    }

    protected void setDeletionCheckbox (Comment comment, boolean checked)
    {
        if (checked) {
            _batchDelete.add(comment);
        } else {
            _batchDelete.remove(comment);
        }
        _batchDelete.updateAbledness();
    }

    protected void complainComment (Comment comment)
    {
        new CommentComplainPopup(comment, _etype).show();
    }

    protected class DeleteClickCallback extends ClickCallback<Integer>
    {
        protected DeleteClickCallback (HasClickHandlers trigger, String confirmMessage)
        {
            super(trigger, confirmMessage);
            updateAbledness();
        }

        public void add (Comment comment)
        {
            _batchComments.put(comment.posted, comment);
            updateAbledness();
        }

        public void remove (Comment comment)
        {
            _batchComments.remove(comment.posted);
            updateAbledness();
        }

        public void clear () {
            _batchComments.clear();
            updateAbledness();
        }

        public void updateAbledness () {
            super.setEnabled(!_batchComments.isEmpty());
        }

        @Override protected boolean callService () {
            _commentsvc.deleteComments(_etype, _entityId,
                new LinkedList<Long>(_batchComments.keySet()), this);
            return true;
        }

        @Override protected boolean gotResult (Integer result) {
            for (Comment comment : _batchComments.values()) {
                removeElement(comment);
            }
            _batchComments.clear();
            return false;
        }

        protected Map<Long, Comment> _batchComments = Maps.newHashMap();
    }

    protected class PostPanel extends BorderedDialog
    {
        public PostPanel (Comment subject) {
            super(false, false, false);
            _subject = subject;
            setHeaderTitle(_cmsgs.commentPostTitle());

            FlowPanel contents = MsoyUI.createFlowPanel("PostComment");
            contents.add(new Label(_cmsgs.commentText()));
            contents.add(_text = new TextArea());
            _text.setWidth("450px");
            _text.setVisibleLines(3);
            contents.add(MsoyUI.createLabel("", "clear")); // forces a line break
            HTML notice = MsoyUI.createHTML(_cmsgs.commentBeNice(), "commentBeNice");
            contents.add(notice);
            SafeHTML.fixAnchors(notice.getElement()); // doctor up the external link
            setContents(contents);

            addButton(new Button(_cmsgs.cancel(), onCancel()));
            addButton(new Button(_cmsgs.send(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    post();
                }
            }));

            _text.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress (KeyPressEvent event) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                        post();
                    }
                }
            });
        }

        protected void post ()
        {
            hide(); // hide now, if they fail validation, they get to type everything anew

            String text = _text.getText().trim();
            if (!text.isEmpty()) {
                postComment(_subject, text);
            }
        }

        public void focus ()
        {
            _text.setFocus(true);
        }

        protected void onClosed (boolean autoClosed)
        {
            _post.setEnabled(true);
        }

        protected TextArea _text;
        protected Comment _subject;
    }

    protected class CommentComplainPopup extends ComplainPopup
    {
        public CommentComplainPopup (Comment comment, CommentType type)
        {
            super(CommentService.MAX_COMPLAINT_LENGTH);
            _comment = comment;
            _type = type;
        }

        protected boolean callService ()
        {
            _commentsvc.complainComment(_description.getText(), _type,
                _comment.entityId, _comment.posted, this);
            return true;
        }

        protected Comment _comment;
        protected CommentType _type;
    }

    protected class ReplyExpander extends ExpanderWidget<Activity>
    {
        public ReplyExpander (Comment comment)
        {
            super(_cmsgs.seeMoreReplies(), false);
            _expandLabel.addStyleName("RepliesExpand");

            _loadingPanel.addStyleName("RepliesLoading");
            _loadingPanel.add(new Image("/images/ui/dot_loader.gif"));

            _comment = comment;
            for (Comment reply : comment.replies) {
                _earliest = Math.min(_earliest, reply.posted);
            }
        }

        protected Widget createElement (Activity activity)
        {
            Comment reply = (Comment) activity;
            _earliest = Math.min(_earliest, reply.posted);
            return createCommentPanel(reply);
        }

        protected void fetchElements (AsyncCallback<ExpanderResult<Activity>> callback)
        {
            _commentsvc.loadReplies(_etype, _comment.entityId,
                _comment.posted, _earliest, 15, callback);
        }

        protected Comment _comment;
        protected long _earliest = Long.MAX_VALUE;
    }

    protected CommentType _etype;
    protected int _entityId;
    protected long _earliest = Long.MAX_VALUE;
    protected Widget _loadingMessage;

    protected DeleteClickCallback _batchDelete;

    protected Panel _commentControls = new HorizontalPanel();

    protected boolean _rated;
    protected Button _post;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final CommentServiceAsync _commentsvc = GWT.create(CommentService.class);
}
