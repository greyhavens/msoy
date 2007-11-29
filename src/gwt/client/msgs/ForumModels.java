//
// $Id$

package client.msgs;

import java.util.List;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.web.client.ForumService;

import client.util.HashIntMap;
import client.util.ServiceBackedDataModel;

/**
 * Various data models used by hte forum services.
 */
public class ForumModels
{
    /** A data model that provides a particular group's threads. */
    public static class GroupThreads extends ServiceBackedDataModel
    {
        public GroupThreads (int groupId) {
            _groupId = groupId;
        }

        public boolean canStartThread () {
            return _canStartThread;
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
            // note all of our threads so that we can provide them later to non-PagedGrid consumers
            for (int ii = 0; ii < tresult.threads.size(); ii++) {
                mapThread((ForumThread)tresult.threads.get(ii));
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
            CMsgs.log("Mapping " + thread.threadId + "...");
            _threads.put(thread.threadId, thread);
        }

        protected int _groupId;
        protected boolean _canStartThread;
        protected HashIntMap _threads = new HashIntMap();
    }

    /** A data model that provides a particular thread's messages. */
    public static class ThreadMessages extends ServiceBackedDataModel
    {
        public ThreadMessages (int threadId) {
            _threadId = threadId;
        }

        public ThreadMessages (ForumThread thread) {
            _threadId = thread.threadId;
            _thread = thread;
            _count = _thread.posts;
        }

        public ForumThread getThread () {
            return _thread;
        }

        public boolean canPostReply () {
            return _canPostReply;
        }

        // @Override // from ServiceBackedDataModel
        public void appendItem (Object item) {
            super.appendItem(item);
            _thread.posts++;
        }

        // @Override // from ServiceBackedDataModel
        public void removeItem (Object item) {
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

            // let the PagedGrid know that we're good and to render the items
            super.onSuccess(result);

            // finally update our thread's last read post id so that subsequent renders will show
            // messages as having been read
            if (mresult.messages.size() > 0) {
                int lastReadIndex = mresult.messages.size()-1;
                int highestPostId = ((ForumMessage)mresult.messages.get(lastReadIndex)).messageId;
                if (highestPostId > _thread.lastReadPostId) {
                    CMsgs.log("Updating last read " + _thread.threadId + " to " + highestPostId);
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
        protected boolean _canPostReply;
    }
}
