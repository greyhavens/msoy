
## Classes relating to defintion of msoy servers

import os.path

class Server:
    '''Represents an msoy server.'''

    def __init__(self, name, num, letter, logstems):
        self._name = name
        self._num = num
        self._tag = "%s%d" % (letter, num)
        self._logstems = logstems

    def privateAddress (self):
        '''The internal network address of the server.'''
        return "%s%d.luna.threerings.net" % (self._name, self._num)

    def brief (self):
        '''Accesses the 2 letter string describing the server, e.g. w2 for whirled2.'''
        return self._tag

    def logstems (self):
        '''Accesses the stem name of the log files, e.g. world-server.'''
        return self._logstems

## World servers
world = map(lambda x: Server("whirled", x, "w", ["world-server"]), [1, 2, 3])

## Game servers
game = map(lambda x: Server("whirled", x, "g", ["game-server"]), [1, 2, 3])

## Bureau servers
bureau = map(lambda x: Server("bureau", x, "b", 
    ["bureau-launcher", "bureau-merged"]), [1, 2])

msoyhome = "/export/msoy"
msoylogs = os.path.join(msoyhome, "log")
