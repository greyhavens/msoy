//
// $Id$

package client.msgs;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.web.client.ForumService;

import com.threerings.gwt.util.SimpleDataModel;

import client.util.HashIntMap;
import client.util.ServiceBackedDataModel;

/**
 * Various data models used by the forum services.
 */
public class ForumModels
{
    /** A data model that provides a particular group's threads. */
    public static class GroupThreads extends ServiceBackedDataModel
    {
        public GroupThreads (int groupId) {
            _groupId = groupId;
            _group = new GroupName("", _groupId);
        }

        public GroupName getGroup () {
            return _group;
        }

        public boolean canStartThread () {
            return _canStartThread;
        }

        public boolean isManager () {
            return _isManager;
        }

        /**
         * Requests to be informed when we obtain our group name from the first batch of thread
         * results. {@link AsyncCallback#onSuccess} will be called with the {@link GroupName} when
         * we learn it. {@link AsyncCallback#onFailure} will never be called but this interface is
         * more convenient than Command which does not allow us to pass an argument.
         */
        public void setGotGroupName (AsyncCallback onGotGroupName) {
            _onGotGroupName = onGotGroupName;
            // if we already have our group name, fire the callback immediately
            if (!_group.toString().equals("")) {
                gotGroupName(_group);
            }
        }

        /**
         * Looks up the specified thread in the set of all threads ever fetched by this model.
         */
        public ForumThread getThread (int threadId)
        {
            return (ForumThread)_threads.get(threadId);
        }

        // @Override // from ServiceBackedDataModel
        public void prependItem (Object item) {
            super.prependItem(item);
            mapThread((ForumThread)item);
        }

        // @Override // from ServiceBackedDataModel
        public void appendItem (Object item) {
            super.appendItem(item);
            mapThread((ForumThread)item);
        }

        // @Override // from ServiceBackedDataModel
        public void onSuccess (Object result) {
            ForumService.ThreadResult tresult = (ForumService.ThreadResult)result;
            _canStartThread = tresult.canStartThread;
            _isManager = tresult.isManager;
            // note all of our threads so that we can provide them later to non-PagedGrid consumers
            for (int ii = 0; ii < tresult.threads.size(); ii++) {
                mapThread((ForumThread)tresult.threads.get(ii));
            }
            // grab our real group name from one of our thread records
            if (tresult.threads.size() > 0) {
                gotGroupName(((ForumThread)tresult.threads.get(0)).group);
            }
            super.onSuccess(result);
        }

        // @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.forumsvc.loadThreads(CMsgs.ident, _groupId, start, count, needCount, this); 
        }

        // @Override // from ServiceBackedDataModel
        protected int getCount (Object result) {
            return ((ForumService.ThreadResult)result).threadCount;
        }

        // @Override // from ServiceBackedDataModel
        protected List getRows (Object result) {
            return ((ForumService.ThreadResult)result).threads;
        }

        protected void mapThread (ForumThread thread) {
            _threads.put(thread.threadId, thread);
        }

        protected void gotGroupName (GroupName group) {
            _group = group;
            if (_onGotGroupName != null) {
                try {
                    _onGotGroupName.onSuccess(_group);
                } catch (Exception e) {
                    CMsgs.log("Got group name callback failed [name=" + group + "].", e);
                }
                _onGotGroupName = null;
            }
        }

        protected int _groupId;
        protected GroupName _group;
        protected boolean _canStartThread, _isManager;

        protected AsyncCallback _onGotGroupName;
        protected HashIntMap _threads = new HashIntMap();
    }

    /** A data model that provides all threads unread by the authenticated user. */
    public static class UnreadThreads extends SimpleDataModel
    {
        public UnreadThreads ()
        {
            super(null);
        }

        /**
         * Looks up the specified thread in the set of all threads ever fetched by this model.
         */
        public ForumThread getThread (int threadId)
        {
            return (ForumThread)_threads.get(threadId);
        }

        // from interface DataModel
        public void removeItem (Object item)
        {
            ForumThread thread = (ForumThread)item;
            _threads.remove(thread.threadId);
            super.removeItem(item);
        }

        // from interface DataModel
        public void doFetchRows (final int start, final int count, final AsyncCallback callback)
        {
            if (_items != null) {
                super.doFetchRows(start, count, callback);
                return;
            }

            CMsgs.forumsvc.loadUnreadThreads(CMsgs.ident, MAX_UNREAD_THREADS, new AsyncCallback() {
                public void onSuccess (Object result) {
                    ForumService.ThreadResult tresult = (ForumService.ThreadResult)result;
                    _items = tresult.threads;
                    for (int ii = 0; ii < _items.size(); ii++) {
                        ForumThread thread = (ForumThread)_items.get(ii);
                        _threads.put(thread.threadId, thread);
                    }
                    doFetchRows(start, count, callback);
                }
                public void onFailure (Throwable failure) {
                    callback.onFailure(failure);
                }
            }); 
        }

        protected HashIntMap _threads = new HashIntMap();
    }

    /** A data model that provides a particular thread's messages. */
    public static class ThreadMessages extends ServiceBackedDataModel
    {
        public ThreadMessages (int threadId, ForumThread thread) {
            _threadId = threadId;
            if (thread != null) {
                _thread = thread;
                _count = _thread.posts;
            }
        }

        public ForumThread getThread () {
            return _thread;
        }

        public boolean canPostReply () {
            return _canPostReply;
        }

        public boolean isManager () {
            return _isManager;
        }

        // @Override // from ServiceBackedDataModel
        public void appendItem (Object item) {
            super.appendItem(item);
            _thread.posts++;
            // mark our thread as read up to this message
            _thread.lastReadPostId = ((ForumMessage)item).messageId;
            _thread.lastReadPostIndex = _thread.posts;
        }

        // @Override // from ServiceBackedDataModel
        public void removeItem (Object item) {
            // if we're deleting the last message in this thread...
            ForumMessage msg = (ForumMessage)item;
            if (_thread.mostRecentPostId == msg.messageId) {
                // ...locate the new last message and update our thread with its info
                int idx = _pageItems.indexOf(item);
                if (idx > 0) { // it's in the list and not the first item
                    ForumMessage prev = (ForumMessage)_pageItems.get(idx-1);
                    _thread.mostRecentPostId = prev.messageId;
                    _thread.mostRecentPoster = prev.poster.name;
                    _thread.mostRecentPostTime = prev.created;
                }
            }

            super.removeItem(item);
            _thread.posts--;
        }

        // @Override // from ServiceBackedDataModel
        public void onSuccess (Object result) {
            // note some bits
            ForumService.MessageResult mresult = (ForumService.MessageResult)result;
            if (mresult.thread != null) {
                _thread = mresult.thread;
            }
            _canPostReply = mresult.canPostReply;
            _isManager = mresult.isManager;

            // let the PagedGrid know that we're good and to render the items
            super.onSuccess(result);

            // finally update our thread's last read post id so that subsequent renders will show
            // messages as having been read
            if (mresult.messages.size() > 0) {
                int lastReadIndex = mresult.messages.size()-1;
                int highestPostId = ((ForumMessage)mresult.messages.get(lastReadIndex)).messageId;
                if (highestPostId > _thread.lastReadPostId) {
                    _thread.lastReadPostId = highestPostId;
                    _thread.lastReadPostIndex = _pageOffset + lastReadIndex;
                }
            }
        }

        // @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.forumsvc.loadMessages(CMsgs.ident, _threadId, _thread.lastReadPostId,
                                        start, count, needCount, this); 
        }

        // @Override // from ServiceBackedDataModel
        protected int getCount (Object result) {
            return ((ForumService.MessageResult)result).thread.posts;
        }

        // @Override // from ServiceBackedDataModel
        protected List getRows (Object result) {
            return ((ForumService.MessageResult)result).messages;
        }

        protected int _threadId;
        protected ForumThread _thread = new ForumThread(); // dummy to make logic easier
        protected boolean _canPostReply, _isManager;
    }

    /**
     * Notifies the cache that a new thread was posted.
     */
    public void newThreadPosted (ForumThread thread)
    {
        // mark this thread as already read
        thread.lastReadPostId = thread.mostRecentPostId;
        thread.lastReadPostIndex = thread.posts;

        // if we already have this model loaded, let it know about the new thread
        GroupThreads gmodel = (GroupThreads)_gmodels.get(thread.group.getGroupId());
        if (gmodel != null) {
            gmodel.prependItem(thread);
        }
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the threads for the
     * specified group.
     */
    public GroupThreads getGroupThreads (int groupId)
    {
        GroupThreads gmodel = (GroupThreads)_gmodels.get(groupId);
        if (gmodel == null) {
            _gmodels.put(groupId, gmodel = new GroupThreads(groupId));
        }
        return gmodel;
    }

    /**
     * Returns, creating if necessary, the data model that provides all unread threads for the
     * authenticated user.
     */
    public UnreadThreads getUnreadThreads (boolean refresh)
    {
        if (refresh || _unreadModel == null) {
            _unreadModel = new UnreadThreads();
        }
        return _unreadModel;
    }

    /**
     * Locates the thread in question in the cache. Returns null if the thread could not be found.
     */
    public ForumThread findThread (int threadId)
    {
        // check for the thread in our unread threads model if we have one
        if (_unreadModel != null) {
            ForumThread thread = _unreadModel.getThread(threadId);
            if (thread != null) {
                return thread;
            }
        }

        // next, check for the thread in the group models
        for (Iterator iter = _gmodels.values().iterator(); iter.hasNext(); ) {
            GroupThreads model = (GroupThreads)iter.next();
            ForumThread thread = model.getThread(threadId);
            if (thread != null) {
                return thread;
            }
        }

        return null;
    }

    /** A cache of GroupThreads data models. */
    protected HashIntMap _gmodels = new HashIntMap();

    /** A cached UnreadThreads data model. */
    protected UnreadThreads _unreadModel;

    /** The maximum number of unread threads we'll download at once. */
    protected static final int MAX_UNREAD_THREADS = 100;
}
