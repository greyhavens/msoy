//
// $Id$

package com.threerings.msoy.fora.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueCodes;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.IssueRecord;
import com.threerings.msoy.fora.server.persist.IssueRepository;

import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation of {@link IssueService}.
 */
public class IssueServlet extends MsoyServiceServlet
    implements IssueService
{
    // from interface IssueService
    public IssueResult loadIssues (boolean open, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        return loadIssues(mrec, open, 0, offset, count, needTotalCount);
    }

    // from interface IssueService
    public IssueResult loadOwnedIssues (boolean open, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        return loadIssues(mrec, open, mrec.memberId, offset, count, needTotalCount);
    }

    // from interface IssueService
    public Issue loadIssue (int issueId)
        throws ServiceException
    {
        IssueRecord irec = _issueRepo.loadIssue(issueId);
        Issue issue = irec.toIssue();
        MemberRecord member = _memberRepo.loadMember(irec.creatorId);
        issue.creator = new MemberName(member.permaName, member.memberId);
        if (irec.ownerId != -1) {
            member = _memberRepo.loadMember(irec.ownerId);
            issue.owner = new MemberName(member.permaName, member.memberId);
        }
        return issue;
    }

    // from interface IssueService
    public ForumMessage loadMessage (int messageId)
        throws ServiceException
    {
        // TODO Do we want to validate read priviledges for this message?
        ForumMessageRecord msgrec = _forumRepo.loadMessage(messageId);
        if (msgrec == null) {
            return null;
        }
        return resolveMessages(Collections.singletonList(msgrec)).get(0);
    }

    // from interface IssueService
    public List<ForumMessage> loadMessages (int issueId, int messageId)
        throws ServiceException
    {
        List<ForumMessageRecord> msgrecs = _forumRepo.loadIssueMessages(issueId);
        if (messageId > 0) {
            ForumMessageRecord msgrec = _forumRepo.loadMessage(messageId);
            msgrecs.add(0, msgrec);
        }
        // TODO Do we want to validate read priviledges for these individual messages?

        return resolveMessages(msgrecs);
    }

    // from interface IssueService
    public Issue createIssue (Issue issue, int messageId)
        throws ServiceException
    {
        requireSupportUser();

        Issue rissue = _issueRepo.createIssue(issue).toIssue();
        rissue.creator = issue.creator;
        rissue.owner = issue.owner;

        if (messageId > 0) {
            _forumRepo.updateMessageIssue(messageId, rissue.issueId);
        }
        return rissue;
    }

    // from interface IssueService
    public Issue updateIssue (Issue issue)
        throws ServiceException
    {
        MemberRecord mrec = requireSupportUser();

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
    }

    // from interface IssueService
    public void assignMessage (int issueId, int messageId)
        throws ServiceException
    {
        requireSupportUser();
        IssueRecord irec = _issueRepo.loadIssue(issueId);
        if (irec.state != Issue.STATE_OPEN) {
            throw new ServiceException(IssueCodes.E_ISSUE_CLOSED);
        }

        _forumRepo.updateMessageIssue(messageId, issueId);
    }

    // from interface IssueService
    public List<MemberName> loadOwners ()
        throws ServiceException
    {
        List<MemberName> owners = Lists.newArrayList();
        List<Integer> gmrIds = _groupRepo.getMemberIdsWithRank(
            ServerConfig.getIssueGroupId(), GroupMembership.RANK_MANAGER);
        List<MemberRecord> members = _memberRepo.loadMembers(gmrIds);
        for (MemberRecord member : members) {
            owners.add(new MemberName(member.permaName, member.memberId));
        }
        return owners;
    }

    protected IssueResult loadIssues (
        MemberRecord mrec, boolean open, int owner, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        IssueResult result = new IssueResult();

        // load up the requested set of issues
        ArrayIntSet states = new ArrayIntSet();
        if (open) {
            states.add(Issue.STATE_OPEN);
        } else {
            states.add(Issue.STATE_RESOLVED);
            states.add(Issue.STATE_IGNORED);
        }
        List<IssueRecord> irecs = _issueRepo.loadIssues(states, owner, offset, count);

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
                result.issues.size() : _issueRepo.loadIssueCount(states);
        }
        return result;
    }

    protected List<ForumMessage> resolveMessages (List<ForumMessageRecord> records)
    {
        // enumerate the posters and create member cards for them
        IntMap<MemberCard> cards = IntMaps.newHashIntMap();
        IntSet posters = new ArrayIntSet();
        for (ForumMessageRecord msgrec : records) {
            posters.add(msgrec.posterId);
        }
        for (MemberCardRecord mcrec : _memberRepo.loadMemberCards(posters)) {
            cards.put(mcrec.memberId, mcrec.toMemberCard());
        }

        // convert the messages to runtime format
        List<ForumMessage> messages = Lists.newArrayList();
        for (ForumMessageRecord msgrec : records) {
            messages.add(msgrec.toForumMessage(cards));
        }
        return messages;
    }

    // our dependencies
    @Inject protected IssueRepository _issueRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ForumRepository _forumRepo;
}
