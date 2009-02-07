
## Classes and procedures used for parsing log files.

import datetime, re

reMergedLine = re.compile('''[wgb]\d+ ''')

dateLength = 23

def getDate (line):
    '''Returns the date stamp if the given string begins with a date stamp. Otherwise returns
    None.'''

    l = line
    if len(l) < 23: return None
    if l[4] != '-' or l[7] != '-' or l[10] != ' ': return None
    if l[13] != ':' or l[16] != ':' or l[19] != ',': return None
    components = [l[0:4], l[5:7], l[8:10], l[11:13], l[14:16], l[17:19], l[20:23]]
    for c in components:
        if not c.isdigit(): return None
    components = map(int, components)
    components[6] *= 1000
    return datetime.datetime(*components)

def getLogDate (line):
    '''Gets the date stamp contained in a log line, or None if the line does not have one.'''

    date = getDate(line)
    if date == None:
        bpos = line.find('| ')
        if bpos >= 1: date = getDate(line[bpos+2:])
    return date

def getMergedLogDate (line):
    d = getDate(line)
    if d == None:
        merged = reMergedLine.match(line)
        if merged != None:
            line = line[len(merged.group()):]
            d = getDate(line)
    return (d, line)

def collectInvokerWarnings (log):
    '''Reads through the log and returns a sequence of InvokerWarning contained therein'''

    reInvokerWarning = re.compile('''WARN Invoker: (Really l|L)ong invoker unit \[unit=(?P<name>.*), time=(?P<time>\d+)ms\]\.''')
    warnings = []
    while True:
        line = log.readline()
        if len(line) == 0: break
        d, line = getMergedLogDate(line)
        if d == None: continue
        warning = reInvokerWarning.search(line)
        if warning == None: continue
        warnings.append(InvokerWarning(d, warning.groupdict()))
    return warnings

class InvokerWarning:
    def __init__(self, date, match):
        self._match = match
        self._date = date

    def name (self):
        return self._match['name']

    def timestamp (self):
        return self._date

    def time (self):
        return int(self._match['time'])

class LogState:
    '''An opened log file with a one line cache so that lines may be merged. There is guaranteed
    to always be one dated line in the cache unless the file has been completely read (isEof).'''

    def __init__(self, file, tag):
        self._file = file
        self._tag = tag
        self._line = None
        self._date = None
        self.read()
        if not self.isEof() and self._date == None:
            raise Exception("First line in file " + self._file.name + " is not a date: " + self._line)

    def isEof (self):
        '''Returns True if the file has been completely read.'''
        return self._line == None

    def date (self):
        '''Returns the date stamp of the cached line.'''
        return self._date

    def read (self):
        '''Reads the next line into the cache.'''
        line = self._file.readline()
        if len(line) == 0:
            self._line = None
            self._date = None
            return
        self._line = line
        self._date = getLogDate(line)

    def flush (self, dest):
        '''Writes out the current cached line and all subsequent non-dated lines.'''
        print >> dest, self._tag, self._line,
        while True:
            self.read()
            if self._date != None: break
            if self.isEof(): break
            print >> dest, self._tag, self._line,

def mergeLogs (files, tags, dest):
    '''Merges a sequence of opened files prefixed with tags to the given destination file.'''

    files = map(LogState, files, tags)
    while True:
        files = filter(lambda f: not f.isEof(), files)
        if len(files) == 0: break
        next = files[0]
        for file in files[1:]:
            if file.date() < next.date(): next = file
        next.flush(dest)


class Event:
    '''An event occurring in a threerings log'''

    def __init__(self, time, line, type, fields):
        '''Creates a new event.'''
        self._type = type
        self._time = time
        self._fields = fields
        self._line = line

    def type (self):
        '''Accesses the type of event, application-specific.'''
        return self._type

    def time (self):
        '''Accesses the time stamp of the event, a datetime object.'''
        return self._time

    def field (self, name):
        '''Accesses a named field of the event, returning None if the field is not found.'''
        return self._fields.get(name, None)

    def line (self):
        '''Accesses the original line parsed to obtain the event.'''
        return self._line

    def __str__ (self):
        kv = self._fields.iteritems()
        return self._type + " [" + ", ".join(["%s=%s" % (k, v) for k, v in kv]) + "]"

class EventTypeParser:
    """Checks a log line for being of a particular event type"""
    def __init__(self, type, lineId, badBracketName=None):
        """Creates a new regex parser."""
        self._type = type
        self._regEx = re.compile(lineId + r" \[(?P<values>.*)\]$")
        self.badBracketName = badBracketName

    def match (self, line):
        """Searches the line for an event id and returns the unique part of the event. This is
        a sequence of name=value pairs separated by a comma in the standard threerings fashion.
        Returns None is the line is not of this event parser's type"""
        m = self._regEx.search(line)
        if m != None: return m.group("values")
        return None

    def type (self):
        """Gets the type of event that this parser will check for."""
        return self._type


reFirstName = re.compile(r"(?P<name>\w+)=")
reSubsequentName = re.compile(r", (?P<name>\w+)=")

def balanced (value, badBracket):
    """Internal method, checks if a string is balanced with open and close round, square and curly
    brackets. This is required because msoy has no formal escaping of values. The bad bracket
    is a boolean indicating that we expect a stray open bracket at the beginning of the string."""
    if badBracket: value = value[1:]
    enclosers = ['[]', '()', '{}']
    stack = []
    for char in value:
        for pair in enclosers:
            if char == pair[0]:
                stack.append(char)
            elif char == pair[1]:
                if len(stack) == 0: return False
                if stack[-1] != pair[0]: return False
                del stack[-1]
    return len(stack) == 0

def parseValues (values, badBracketName):
    """Parses a sequence of name=value pairs separated by commas and returns a dictionary mapping.
    The bad bracket name indicates the name that will appear with a stray open bracket as in
    name=[value"""
    #print "Parsing values for %s" % values
    fields, m, pos = ({}, reFirstName.match(values), 0)
    while True:
        if m == None: break
        name, pos, spos = (m.group('name'), m.end(), m.end())
        # we have a second loop here so we can tell when we've reached the end of a value
        # that uses the same separator as the pairs themselves like foo=(x, y, z), bar=...
        # this is kind of cheesy and we should just be able to balance parentheses but
        # user input names occur unescaped in the logs
        while True:
            m = reSubsequentName.search(values, spos)
            if m == None:
                fields[name] = values[pos:]
                break
            if balanced(values[pos:m.start()], name == badBracketName):
                fields[name] = values[pos:m.start()]
                pos = m.start()
                break
            spos = m.start() + 2
    return fields

def extractEvent (line, parsers):
    """Attempts to extract an event from the given log line with the given list of parsers.
    Returns None if no parser matched."""
    date, content = getMergedLogDate(line)
    if date == None: return None
    event = None
    for parser in parsers:
        fields = parser.match(content)
        if fields == None: continue
        fields = parseValues(fields, parser.badBracketName)
        return Event(date, content, parser.type(), fields)
    return None

def enumerateEvents (file, parsers):
    """Reads in the lines of the given file and generates all found events."""
    while True:
        line = file.readline()
        if len(line) == 0: break
        event = extractEvent(line, parsers)
        if event != None: yield event

