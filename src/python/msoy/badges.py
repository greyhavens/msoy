
import re, sys

## AUTO GENERATED from com.threerings.msoy.badge.data.BadgeType
codesToName = {
    -990018741: 'FRIENDLY',
    -94886133: 'MAGNET',
    983613172: 'FIXTURE',
    567029922: 'EXPLORER',
    2138102039: 'GAMER',
    -425662117: 'CONTENDER',
    -1978012765: 'COLLECTOR',
    1852244093: 'CHARACTER_DESIGNER',
    -255838771: 'FURNITURE_BUILDER',
    292647383: 'LANDSCAPE_PAINTER',
    646396602: 'PROFESSIONAL',
    52819145: 'ARTISAN',
    421773639: 'SHOPPER',
    -424738396: 'JUDGE',
    1017487473: 'OUTSPOKEN',
}

class Action:
    def __init__(self, server, type, match):
        self._server = server
        self._type = type
        self._match = match

    def type (self):
        return self._type

    def match (self):
        return self._match

    def badgeName (self):
        if self.type() == 'deleted':
            return codesToName[int(self._match.group("code"))]
        if self.type() == 'stored':
            return self._match.group("name")
        raise Exception()

    def nextLevel (self):
        if self.type() == 'deleted': return None
        return int(self._match.group('nextLevel'))

    def progress (self):
        if self.type() == 'deleted': return None
        return float(self._match.group('progress'))

    def server (self):
        return self._server

class MemberLog:

    def __init__ (self, memberId):
        self._memberId = memberId
        self._actions = []

    def memberId (self):
        return self._memberId

    def actions (self):
        return self._actions

    def addAction (self, action):
        self._actions.append(action)

def processInProgressBadgeWrites (input):
    prefix = "INFO BadgeRepository: "
    reDeleted = [
        re.compile('''^(?P<server>[gw]\d)?.*%sDeleting in-progress badge''' % prefix),
        re.compile('''memberId=(?P<member>\d+), badgeCode=(?P<code>[\d-]+)''')]
    reStored = [
        re.compile('''^(?P<server>[gw]\d)?.*%sStoring in-progress badge''' % prefix),
        re.compile('''badge=memberId=(?P<member>\d+) BadgeType=(?P<name>[A-Z_]+) nextLevel=(?P<nextLevel>\d+) progress=(?P<progress>[\d.-]+)''')]

    members = {}

    def maybeLogAction (type, exps, line):
        m = exps[0].search(line)
        if m == None: return
        server = m.group('server')
        m = exps[1].search(line)
        if m == None:
            print >> sys.stderr, "Line failed to match detailed expression:", line,
            return
        memberId = int(m.group("member"))
        member = members.get(memberId, None)
        if member == None:
            member = MemberLog(memberId)
            members[memberId] = member
        member.addAction(Action(server, type, m))
        return member

    while True:
        line = input.readline()
        if len(line) == 0: break
        maybeLogAction('deleted', reDeleted, line)
        maybeLogAction('stored', reStored, line)

    return members

