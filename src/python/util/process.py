
## Classes and procedures related to running and reading output from processes

import subprocess

class ProgramError(Exception):
    '''A problem that occurred due to running an external program.'''

    def __init__(self, result):
        self._result = result

    def result (self):
        '''The result of running the program.'''
        return self._result

    def dump (self, file):
        '''Writes extended error information to a file.'''
        print >> file, "ERROR: Program execution failed"
        self.result().dump(file, "  ")

class Result:
    '''The result of running an external program.'''

    def __init__ (self, args, stdout, exitCode):
        self._args = args
        self._stdout = stdout
        self._exitCode = exitCode

    def exitCode (self):
        '''Accesses the exit code returned by the program.'''
        return self._exitCode

    def stdout (self):
        '''Accesses the sequence of lines printed by the program.'''
        return self._stdout

    def args (self):
        '''Accesses the arguments used to invoke the program.'''
        return self._args

    def dump (self, file, prefix):
        '''Writes the complete information of this result to a file, with a prefix on each line.'''
        error = ["Arguments:"]
        error += map(lambda s: "  %s" % s, self.args())
        error += ["Output:"]
        error += map(lambda s: "  %s" % s.rstrip(), self.stdout())
        error += ["Return code: %d" % self.exitCode()]
        for e in error: print >> file, "%s%s" % (prefix, e)

def capture (args):
    '''Launches a progam, waits for it to exit and returns the result object. The 0th element of
    the args if the program to run, the rest are the program arguments.'''

    PIPE = subprocess.PIPE
    STDOUT = subprocess.STDOUT
    proc = subprocess.Popen(args, stdout=PIPE, stderr=STDOUT)
    stdout = []
    while True:
        line = proc.stdout.readline()
        if len(line) == 0: break
        stdout.append(line)
    ret = proc.wait()
    return Result(args, stdout, ret)

