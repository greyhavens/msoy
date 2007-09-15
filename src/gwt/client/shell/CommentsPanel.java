//
// $Id$

package client.shell;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.fora.data.Comment;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends VerticalPanel
{
    public CommentsPanel (int entityType, int entityId)
    {
        setStyleName("commentsPanel");

        _entityType = entityType;
        _entityId = entityId;

        add(MsoyUI.createLabel("Comments", "Header"));
        add(_comments = new VerticalPanel());
        _comments.setSpacing(5);

        // TODO: add "previous" and "next" links/buttons

        // if we're logged in, display the posting UI
        if (CShell.getMemberId() != 0) {
            add(_post = new Button("Post comment...", new ClickListener() {
                public void onClick (Widget sender) {
                    _post.setEnabled(false);
                    add(new PostPanel());
                }
            }));
        }

        setPage(0); // start on page zero of our comments
    }

    public void setPage (final int page)
    {
        int offset = page * COMMENTS_PER_PAGE, count = COMMENTS_PER_PAGE;
        _page = page;
        CShell.commentsvc.loadComments(_entityType, _entityId, offset, count, new AsyncCallback() {
            public void onSuccess (Object result) {
                _comments.clear();
                List comments = (List)result;
                for (int ii = 0; ii < comments.size(); ii++) {
                    _comments.add(new CommentPanel(CommentsPanel.this, (Comment)comments.get(ii)));
                }
                if (comments.size() == 0) {
                    _comments.add(MsoyUI.createLabel("No comments.", "Status"));
                }
            }

            public void onFailure (Throwable cause) {
                _comments.clear();
                _comments.add(MsoyUI.createLabel(CShell.serverError(cause), "Status"));
            }
        });
    }

    protected void postComment (String text)
    {
        CShell.commentsvc.postComment(
            CShell.ident, _entityType, _entityId, text, new AsyncCallback() {
            public void onSuccess (Object result) {
                postedComment((Comment)result);
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    protected void postedComment (Comment comment)
    {
        if (_page == 0) {
            int children = _comments.getWidgetCount();
            if (children == COMMENTS_PER_PAGE) {
                _comments.remove(COMMENTS_PER_PAGE-1);
            }
            // clear out the "No comments" label if we have one
            if (children > 0 && !(_comments.getWidget(0) instanceof CommentPanel)) {
                _comments.clear();
            }
            // stick this comment at the top of the list like it's the real deal
            _comments.insert(new CommentPanel(this, comment), 0);

        } else {
            MsoyUI.info("Comment posted. Click 'Latest' to see it.");
        }
    }

    protected void deleteComment (final CommentPanel panel, Comment comment)
    {
        CShell.commentsvc.deleteComment(
            CShell.ident, _entityType, _entityId, comment.posted, new AsyncCallback() {
            public void onSuccess (Object result) {
                MsoyUI.info("Comment deleted.");
                _comments.remove(panel);
                if (_page == 0 && _comments.getWidgetCount() == 0) {
                    _comments.add(MsoyUI.createLabel("No comments.", "Status"));
                }
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    protected class PostPanel extends VerticalPanel
    {
        public PostPanel () {
            add(new Label("Enter the text of your comment:"));
            add(_text = new TextArea());
            _text.setCharacterWidth(40);
            _text.setVisibleLines(3);
            add(_status = new Label(""));
            RowPanel buttons = new RowPanel();
            buttons.add(new Button("Cancel", new ClickListener() {
                public void onClick (Widget sender) {
                    clearPost();
                }
            }));
            buttons.add(new Button("Post", new ClickListener() {
                public void onClick (Widget sender) {
                    postComment(_text.getText());
                    clearPost();
                }
            }));
            add(buttons);
        }

        protected void clearPost () {
            CommentsPanel.this.remove(this);
            _post.setEnabled(true);
        }

        protected TextArea _text;
        protected Label _status;
    }

    protected int _entityType, _entityId;
    protected int _page;

    protected VerticalPanel _comments;
    protected Button _post;

    protected static final int COMMENTS_PER_PAGE = 10;
}
