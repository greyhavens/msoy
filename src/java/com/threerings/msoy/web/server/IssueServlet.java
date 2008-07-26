//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.IssueRecord;
import com.threerings.msoy.fora.server.persist.IssueRepository;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueCodes;

import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.web.client.IssueService;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;
import com.threerings.msoy.server.persist.MemberCardRecord;

/**
 * Provides the server implementation of {@link IssueService}.
 */
public class IssueServlet extends MsoyServiceServlet
    implements IssueService
{
    // from interface IssueService
    public IssueResult loadIssues (
            WebIdent ident, int type, int state, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        return loadIssues(mrec, type, state, 0, offset, count, needTotalCount);
    }

    // from interface IssueService
    public IssueResult loadOwnedIssues (
            WebIdent ident, int type, int state, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);
        return loadIssues(mrec, type, state, mrec.memberId, offset, count, needTotalCount);
    }

    // from interface IssueService
    public Issue loadIssue (WebIdent ident, int issueId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            IssueRecord irec = _issueRepo.loadIssue(issueId);
            Issue issue = irec.toIssue();
            MemberRecord member = _memberRepo.loadMember(irec.creatorId);
            issue.creator = new MemberName(member.permaName, member.memberId);
            if (irec.ownerId != -1) {
                member = _memberRepo.loadMember(irec.ownerId);
                issue.owner = new MemberName(member.permaName, member.memberId);
            }
            return issue;
        } catch (PersistenceException pe) {
            log.warning("Failed to load issue [for=" + who(mrec) +
                    ", issueId=" + issueId + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface IssueService
    public List<ForumMessage> loadMessages (WebIdent ident, int issueId, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            List<ForumMessageRecord> msgrecs = _forumRepo.loadIssueMessages(issueId);
            if (messageId > 0) {
                ForumMessageRecord msgrec = _forumRepo.loadMessage(messageId);
                msgrecs.add(0, msgrec);
            }
            // TODO Do we want to validate read priviledges for these individual messages?

            // enumerate the posters and create member cards for them
            IntMap<MemberCard> cards = IntMaps.newHashIntMap();
            IntSet posters = new ArrayIntSet();
            for (ForumMessageRecord msgrec : msgrecs) {
                posters.add(msgrec.posterId);
            }
            for (MemberCardRecord mcrec : _memberRepo.loadMemberCards(posters)) {
                cards.put(mcrec.memberId, mcrec.toMemberCard());
            }

            // convert the messages to runtime format
            List<ForumMessage> messages = Lists.newArrayList();
            for (ForumMessageRecord msgrec : msgrecs) {
                messages.add(msgrec.toForumMessage(cards));
            }
            return messages;

        } catch (PersistenceException pe) {
            log.warning("Failed to load issue messages [for=" + who(mrec) +
                    ", issueId=" + issueId + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface IssueService
    public Issue createIssue (WebIdent ident, Issue issue, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            if (!mrec.isSupport()) {
                throw new ServiceException(IssueCodes.E_ACCESS_DENIED);
            }

            Issue rissue = _issueRepo.createIssue(issue).toIssue();
            rissue.creator = issue.creator;
            rissue.owner = issue.owner;

            if (messageId > 0) {
                _forumRepo.updateMessageIssue(messageId, rissue.issueId);
            }
            return rissue;

        } catch (PersistenceException pe) {
            log.warning("Failed to create issue [for=" + who(mrec) +
                    ", messageId=" + messageId + ", description=" + issue.description + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface IssueService
    public Issue updateIssue (WebIdent ident, Issue issue)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            if (!mrec.isSupport()) {
                throw new ServiceException(IssueCodes.E_ACCESS_DENIED);
            }
            IssueRecord irec = _issueRepo.loadIssue(issue.issueId);
            if (irec.state != Issue.STATE_OPEN) {
                throw new ServiceException(IssueCodes.E_ISSUE_CLOSED);
            }
            if (issue.state == Issue.STATE_OPEN) {
                issue.closeComment = null;
            } else if (issue.owner == null) {
                throw new ServiceException(IssueCodes.E_ISSUE_CLOSE_NO_OWNER);
            } else if (issue.owner.getMemberId() != mrec.memberId) {
                throw new ServiceException(IssueCodes.E_ISSUE_CLOSE_NOT_OWNER);
            }

            _issueRepo.updateIssue(issue);
            return issue;

        } catch (PersistenceException pe) {
            log.warning("Failed to update issue [for=" + who(mrec) +
                    "issueId=" + issue.issueId + ", description=" + issue.description + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface IssueService
    public void assignMessage (WebIdent ident, int issueId, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            if (!mrec.isSupport()) {
                throw new ServiceException(IssueCodes.E_ACCESS_DENIED);
            }
            IssueRecord irec = _issueRepo.loadIssue(issueId);
            if (irec.state != Issue.STATE_OPEN) {
                throw new ServiceException(IssueCodes.E_ISSUE_CLOSED);
            }

            _forumRepo.updateMessageIssue(messageId, issueId);
        } catch (PersistenceException pe) {
            log.warning("Failed to assign message [for=" + who(mrec) +
                    "issueId=" + issueId + ", messageId=" + messageId + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface IssueService
    public List<MemberName> loadOwners (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        List<MemberName> owners = Lists.newArrayList();

        try {
            List<GroupMembershipRecord> gmrs = _groupRepo.getMembers(
                ServerConfig.getIssueGroupId(), GroupMembership.RANK_MANAGER);
            ArrayIntSet memberIds = new ArrayIntSet();
            for (GroupMembershipRecord gmr : gmrs) {
                memberIds.add(gmr.memberId);
            }
            List<MemberRecord> members = _memberRepo.loadMembers(memberIds);
            for (MemberRecord member : members) {
                owners.add(new MemberName(member.permaName, member.memberId));
            }
        } catch (PersistenceException pe) {
            log.warning("Failed to load owners [for=" + who(mrec) + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
        return owners;
    }

    protected IssueResult loadIssues (MemberRecord mrec, int type, int state, int owner, int offset,
            int count, boolean needTotalCount)
        throws ServiceException
    {
        try {
            IssueResult result = new IssueResult();

            // load up the requested set of issues
            ArrayIntSet types = new ArrayIntSet();
            types.add(type);
            ArrayIntSet states = new ArrayIntSet();
            states.add(state);
            List<IssueRecord> irecs =
                _issueRepo.loadIssues(types, states, owner, offset, count);

            List<Issue> issues = Lists.newArrayList();
            if (irecs.size() > 0) {
                IntMap<MemberName> mnames = IntMaps.newHashIntMap();
                IntSet members = new ArrayIntSet();

                for (IssueRecord record : irecs) {
                    members.add(record.creatorId);
                    if (record.ownerId != -1) {
                        members.add(record.ownerId);
                    }
                }
                for (MemberRecord mem : _memberRepo.loadMembers(members)) {
                    mnames.put(mem.memberId, new MemberName(mem.permaName, mem.memberId));
                }
                for (IssueRecord record : irecs) {
                    Issue issue = record.toIssue();
                    issue.creator = mnames.get(record.creatorId);
                    if (record.ownerId != -1) {
                        issue.owner = mnames.get(record.ownerId);
                    }
                    issues.add(issue);
                }
            }
            result.issues = issues;

            if (needTotalCount) {
                result.issueCount = (result.issues.size() < count && offset == 0) ?
                    result.issues.size() : _issueRepo.loadIssueCount(types, states);
            }
            return result;

        } catch (PersistenceException pe) {
            log.warning("Failed to load issues [for=" + who(mrec) + ", type=" + type +
                    ", state=" + state + ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(IssueCodes.E_INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected IssueRepository _issueRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ForumRepository _forumRepo;
}
