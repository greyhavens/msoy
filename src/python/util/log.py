
## Classes and procedures used for parsing log files.

import datetime

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
