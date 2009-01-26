#!/usr/bin/python

import re, httplib, time, operator, datetime, optparse, sys

##
## Classes, procedures and functions related to parsing http://www.whirled.com/report/status 
##


gServer = "www.whirled.com"
gReport = "/status/report"

class Report:
    '''The top level object for the status page. Contains a list of nodes, sorted by node number'''

    def __init__ (self):
        self._nodes = []

    def addNode (self, node):
        '''Appends a new node, used when parsing the status page.'''
        self._nodes.append(node)
        self._nodes.sort(cmp=lambda a,b: a.num() - b.num())

    def nodes (self):
        '''Accesses the list of nodes in the report.'''
        return self._nodes

class Node:
    '''Data for a single node (server process) in the report, e.g. msoy1 world server'''

    def __init__(self, num, game, summary):
        self._num = num
        self._game = game
        self._queues = []
        self._lines = []
        self._summary = {}
        if summary != None:
            for kv in summary.split(", "):
                pair = kv.split("=")
                self._summary[pair[0]] = int(pair[1])

    def num (self):
        '''The number of the node, e.g. msoy1 has number 1.'''
        return self._num

    def game (self):
        '''Whether the node is a game server.'''
        return self._game

    def addQueue (self, queue):
        '''Appends a new queue to the node, used when parsing the status page.'''
        self._queues.append(queue)

    def addLine (self, line):
        '''Adds a new line to the node's extended summary section, used when parsing the report.
        Node lines are not parsed until needed.'''
        self._lines.append(line)

    def queues (self):
        '''Accesses the list of queues in the node.'''
        return self._queues

    def summary (self):
        '''The summary dictionary of name=value pairs that appear in the one-line node summary.'''
        return self._summary

class Queue:
    '''Data for a queue within a node, e.g. presents.Invoker and presents.ClientManager.'''

    def __init__(self, name):
        self._name = name
        self._queues = []
        self._lines = []

    def name (self):
        '''Accesses the name of the queue.'''
        return self._name

    def addLine (self, line):
        '''Adds a line to the queue, used when parsing the report. Queue lines are not parsed until
        needed'''
        self._lines.append(line)

    def lines (self):
        '''Accesses the lines reported by this queue.'''
        return self._lines

def numbersWithChange (current, last, wid, chwid, extra=None):
    '''Constructs a summary string showing N current values and their changes from previous values.
    e.g. if a number changes from 4 to 10, "10 [+6]" would be returned. The wid and chwid parameters
    control the field width and change field width, respectively. The chwid should include space for
    a sign value. The optional extra parameter is shown as a secondary change, e.g. if a number
    changes from 4 to 10 with an extra 2, "10 [+6/+2]" is returned.'''

    if wid != None: fmt = "%%%dd" % wid
    else: fmt = "%d"

    if chwid != None:
        if extra != None: fmtc = " [%%+%dd/%%+%dd]" % (chwid, chwid)
        else: fmtc = " [%%+%dd]" % chwid
    else: fmtc = " [%+d]"

    change = map(operator.sub, current, last)
    if extra != None: change = map(lambda c,e: fmtc % (c,e), change, extra)
    else: change = map(lambda c: fmtc % c, change)
    current = map(lambda c: fmt % c, current)
    return " ".join(map(operator.add, current, change))

class State:
    '''Represents the current state of the server for the purposes of monitoring invoker queue
    lengths, units and user counts.'''

    def __init__(self, queueSizes, units, users, lastState):
        self._queueSizes = queueSizes
        self._users = users
        self._lastState = lastState
        self._units = units
        self._timeStamp = datetime.datetime.now()

    def queueSizes (self):
        '''Accesses the sequence of queue sizes, one per node.'''
        return self._queueSizes

    def users (self):
        '''Accesses the number of users.'''
        return self._users

    def totalUnits (self):
        '''Accesss the total number of units executed.'''
        return reduce(operator.add, self._units)

    def units (self):
        '''Accesses the sequence of numbers of units executed, one per node.'''
        return self._units

    def lastQueueSizes (self):
        '''Access the queue sizes of the previous state. If there is no previous state, returns
        this state's queue sizes.'''
        if self._lastState == None: return self.queueSizes()
        return self._lastState.queueSizes()

    def lastUsers (self):
        '''Access the user count of the previous state. If there is no previous state, returns
        this state's user count.'''
        if self._lastState == None: return self.users()
        return self._lastState.users()

    def lastTotalUnits (self):
        '''Access the total units of the previous state. If there is no previous state, returns
        this state's total units.'''
        if self._lastState == None: return self.totalUnits()
        return self._lastState.totalUnits()

    def lastUnits (self):
        '''Access the units executed of the previous state. If there is no previous state, returns
        this state's units.'''
        if self._lastState == None: return self.units()
        return self._lastState.units()

    def timestamp (self):
        '''Accesses the timestamp of this state.'''
        return self._timeStamp

def getReport ():
    '''Reads the web status report from whirled.com, parses the result and returns a fully
    populated Report object.'''

    ## e.g. * presents.Invoker
    reQueue = re.compile('''\* (?P<name>[a-zA-Z.]+)''')

    ## e.g. msoy3 [members=354, guests=28, inScene=250, inGame=122, rooms=208, games=23, channels=1]
    ## or   msoy3 game:47625
    reNode = re.compile('''^msoy(?P<num>\d) (?P<game>game)?(\[(?P<summary>.*)\])?''')

    # read the web page
    conn = httplib.HTTPConnection(gServer)
    conn.request("GET", gReport)
    response = conn.getresponse()
    if response.status != 200:
        raise httplib.HTTPException("Could not get status page, response=" + response)
    data = response.read()
    
    # set up parsing variables
    report = Report()
    node = None
    queue = None

    # parse lines
    for line in data.split("\n"):
        m = reNode.match(line)
        if m != None:
            node = Node(int(m.group('num')), m.group('game') != None, m.group('summary'))
            queue = None
            report.addNode(node)
            continue
    
        m = reQueue.match(line)
        if node != None and m != None:
            queue = Queue(m.group('name'))
            node.addQueue(queue)
            continue
    
        if queue != None: queue.addLine(line)
        elif node != None: node.addLine(line)
        else: print line

    return report

def filterLines (report, nodeFilter, queueFilter, lineFilter):
    '''Returns all lines matching the line filter function. Only lines from queues matching the
    queue filter function are considered. Only queues from nodes matching the node filter function
    are considered.'''

    nodes = filter(nodeFilter, report.nodes())
    queues = reduce(lambda qs,n: qs + n.queues(), nodes, [])
    queues = filter(queueFilter, queues)
    lines = reduce(lambda ls,q: ls + q.lines(), queues, [])
    lines = filter(lineFilter, lines)
    return lines

def getState (report, lastState):
    '''Generate a state from a report.'''

    ## e.g. - Queue size: 58
    reQueueSize = re.compile('''^- Queue size: (?P<size>\d+)''')

    ## e.g. - Units executed: 38297 (0/s)
    reUnits = re.compile('''^- Units executed: (?P<count>\d+)''')

    ## extract the main invoker queue size line for each node
    sizes = filterLines(report,
        lambda n: not n.game(),
        lambda q: q.name() == 'presents.Invoker',
        lambda l: reQueueSize.match(l) != None)

    ## extract the size from the lines and convert to int
    sizes = map(lambda l: int(reQueueSize.match(l).group("size")), sizes)

    ## extract the main invoker units executed line for each node
    units = filterLines(report,
        lambda n: not n.game(),
        lambda q: q.name() == 'presents.Invoker',
        lambda l: reUnits.match(l) != None)

    ## extract the count and convert to int
    units = map(lambda l: int(reUnits.match(l).group("count")), units)

    ## add up the number of users for each node
    nodes = filter(lambda n: not n.game(), report.nodes())
    users = reduce(lambda c,n: c + n.summary()['members'] + n.summary()['guests'], nodes, 0)

    ## create and return the state
    return State(sizes, units, users, lastState)

def describeState (st):
    '''Constructs a string describing the state.'''

    ## e.g.
    ## Mon 14:21:09  Queues:   13 [ +13/ +79]    2 [  +2/ +62]  Users: 1275 [ +3]  Units: 1782592 [+141]

    units = map(operator.sub, st.units(), st.lastUnits())
    return st.timestamp().strftime("%a %H:%M:%S") + "  Queues: " + \
        numbersWithChange(st.queueSizes(), st.lastQueueSizes(), 4, 4, units) + \
        "  Users: " + numbersWithChange([st.users()], [st.lastUsers()], 4, 3) + \
        "  Units: " + numbersWithChange([st.totalUnits()], [st.lastTotalUnits()], 7, 4)

def main (args):
    '''Parses command line arguments and monitors the whirled status. Use main(["-h"]) for more
    information.'''

    description = '''Monitors http://%s/%s and prints a summary of the invoker queues and user
 counts at an interval. Each line includes a date stamp, followed by the queue sizes on each
 node, then the total user count total number of invoker units executed on all nodes. The
 change in a value since the last sample is shown in brackets. Queue sizes in addition show
 the number of invoker units executed since the last sample. If the number of nodes changes
 or the server goes down, the delta values reset.''' % (gServer, gReport)

    parser = optparse.OptionParser(description=description)
    parser.add_option("-e", "--every", dest="sleep", default=5,
        help="interval at which to print status lines", metavar="SECONDS")

    (opts, args) = parser.parse_args(args)

    state = None
    while True:
        report = None
        try:
            report = getReport()
        except Exception, ex:
            print "** Server not responding: " + str(ex)
        if report != None:
            state = getState(report, state)
            if len(state.queueSizes()) != len(state.lastQueueSizes()):
                state._lastState = None
                print "** Nodes changed"
            print describeState(state)
            state._lastState = None
        try: time.sleep(int(opts.sleep))
        except KeyboardInterrupt: break
