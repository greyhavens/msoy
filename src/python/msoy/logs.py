#
# $Id$

## Methods for downloading msoy logs to the TEMP directory

import util.process as procutil, msoy.servers
import os, os.path, sys

def resolveLog (server, remoteFile, force):
    '''Downloads the specified log file from the specified server and returns the local path.
    If force is set, the file is always downloaded, overwriting the local file if any. Otherwise,
    the file is only downloaded if the local file does not exist.'''

    base = os.path.basename(remoteFile)
    user = os.environ['USER']
    tmp = '/tmp'

    local = os.path.join(tmp, "%s.%s" % (server.privateAddress(), base))
    if force or not os.path.exists(local):
        listing = procutil.capture(["ssh", server.privateAddress(), "ls", remoteFile])
        if listing.exitCode() != 0:
            print >> sys.stderr, "File %s not found on %s using ssh, skipping" % (
                remoteFile, server.privateAddress())
            return None
        else:
            print >> sys.stderr, "Retrieving %s to %s" % (remoteFile, local)
            result = procutil.capture(["rsync", "%s@%s:%s" % (user, server.privateAddress(), remoteFile), local])
            if result.exitCode() != 0: raise procutil.ProgramError(result)
    else:
        print >> sys.stderr, "Using previously retrieved file %s" % local
    return local

def retrieveLogs (servers, stem, date, force):
    '''Retrieves the same log file from each given server.'''

    if date == None: date = ""
    else: date = ".%s" % date
    files = []
    for server in servers:
        local = resolveLog(server, os.path.join(
            msoy.servers.msoylogs, "%s.log%s" % (stem, date)), force)
        if local != None: files.append(local)
    return files


