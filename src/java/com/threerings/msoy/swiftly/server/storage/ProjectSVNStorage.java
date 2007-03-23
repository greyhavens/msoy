//
// $Id$

package com.threerings.msoy.swiftly.server.storage;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.server.persist.SwiftlySVNStorageRecord;

import com.threerings.msoy.web.data.SwiftlyProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;

import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;

import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;

/**
 * Handles the subversion-based project repository.
 */
public class ProjectSVNStorage
    implements ProjectStorage
{

    /** Standard SVN Protocol String. */
    public static final String PROTOCOL_SVN = "svn";

    /** SVN over SSH Protocol String. */
    public static final String PROTOCOL_SSH = "svn+ssh";
    
    /** File-local SVN Protocol String. */
    public static final String PROTOCOL_FILE = "file";

    /** SVN over HTTPS WebDAV Protocol String. */
    public static final String PROTOCOL_HTTPS = "https";


    /*
     * Static initialization of the SVNKit library
     */
    static {
        // Initialize SVNKit repository protocol factories

        // http:// and https://
        DAVRepositoryFactory.setup();

        // svn:// and svn+...://
        SVNRepositoryFactoryImpl.setup();

        // file:///
        FSRepositoryFactory.setup();
    }


    /**
     * Initialize project storage for a given project record. Any .svn directories in the given
     * template directory will be silently ignored.
     * TODO: Destroy the repository directory on failure.
     */
    public static ProjectSVNStorage initializeStorage (SwiftlyProject project,
        SwiftlySVNStorageRecord storageRecord, File templateDir)
        throws ProjectStorageException
    {
        ProjectSVNStorage storage;
        SVNRepository svnRepo;
        ISVNEditor editor;
        SVNCommitInfo commitInfo;
        long latestRevision;

        // If this is a local repository, we'll attempt to create it now.
        if (storageRecord.protocol.equals(PROTOCOL_FILE)) {
            File repoDir = new File(storageRecord.baseDir, Integer.toString(project.projectId));
            try {
                SVNRepositoryFactory.createLocalRepository(repoDir, true, false);                
            } catch (SVNException e) {
                throw new ProjectStorageException.InternalError("Failure creating local project " +
                    "repository: " + e, e);                
            }
        }

        // Connect to the repository
        storage = new ProjectSVNStorage(project, storageRecord);

        // If the revision is not 0, this is not a new project. Exit immediately.
        try {
            svnRepo = storage.getSVNRepository();
            latestRevision = svnRepo.getLatestRevision();
            if (latestRevision != 0) {
                throw new ProjectStorageException.ConsistencyError("Invalid request to initialize a " +
                    "previously initialized storage repository");
            }
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Failure retrieving project " +
                "revision: " + e, e);
        }


        // New project, set up our commit editor
        try {
            editor = svnRepo.getCommitEditor("Swiftly Project Initialization", null);
        } catch (SVNException e) {
            throw new ProjectStorageException.InternalError("Failure initializing commit editor: "
                + e, e);
        }

        // "svn add" the template.
        try {
            // Open the repository root.
            editor.openRoot(-1);           

            // Add the template directory to the commit.
            svnAddDirectory(editor, "", templateDir);

            // Close the repository root.
            editor.closeDir();

            // Commit the whole lot.
            commitInfo = editor.closeEdit();
    
        } catch (SVNException e) {
            try {
                // We have to abort the open edit. It can also raise an SVNException!
                editor.abortEdit();                
            } catch (SVNException eabort) {
                throw new ProjectStorageException.InternalError("Failure aborting subversion commit: "
                    + eabort, eabort);                
            }
            
            // Report failure.
            throw new ProjectStorageException.InternalError("Failure committing project template: "
                + e, e);

        } catch (FileNotFoundException fnfe) {
            // Either someone handed us a bad template directory, or someone removed things from it
            // while we were running.
            throw new ProjectStorageException.ConsistencyError("Could not load template: "
                + fnfe, fnfe);
        } catch (IOException ioe) {
            throw new ProjectStorageException.InternalError("Could not load template, failure reading input file:" +
                ioe, ioe);
        }

        // Validate the commit.
        if (commitInfo == null) {
            throw new ProjectStorageException.InternalError("Subversion commit failed, null commit info returned");            
        }

        if (commitInfo.getNewRevision() != latestRevision + 1) {
            throw new ProjectStorageException.InternalError("Subversion commit failed: " + commitInfo.getErrorMessage());
        }

        // Everything worked, return the now-initialized storage instance.
        return storage;
    }


    /** 
     * Construct a new storage instance for the given project record.
     */
    public ProjectSVNStorage (SwiftlyProject project, SwiftlySVNStorageRecord storageRecord)
        throws ProjectStorageException
    {
        _project = project;
        _storageRecord = storageRecord;

        // Initialize subversion magic
        try {
            _svnURL = getSVNURL();
            // TODO -- Remote authentication manager
            _svnPool = new DefaultSVNRepositoryPool(null, null, true, DefaultSVNRepositoryPool.INSTANCE_POOL);
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("Could not parse subversion URL: " + svne, svne);
        } catch (URISyntaxException urie) {
            throw new ProjectStorageException.InternalError(
                "Invalid URL provided by SwiftlySVNStorageRecord: " + urie, urie);            
        }
    }


    // from interface ProjectStorage
    // Recurse over the entirity of the subversion repository, building a list
    // of project path elements.
    // TODO: Extend the Swiftly client to support readdir()-style functionality instead of this sillyness.
    public List<PathElement> getProjectTree ()
        throws ProjectStorageException
    {
        SVNRepository svnRepo;

        // Stat the project root.
        try {
            svnRepo = getSVNRepository();
            SVNNodeKind nodeKind = svnRepo.checkPath("", -1);

            // Are we starting at a directory?
            if (nodeKind != SVNNodeKind.DIR) {
                throw new ProjectStorageException.InternalError("The subversion root path is not a directory");
            }
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("Unable to stat() the project root path: " + svne, svne);
        }

        try {
            long latestRevision = svnRepo.getLatestRevision();
            PathElement root = PathElement.createRoot(_project.projectName);
            List<PathElement> elements = recurseTree(svnRepo, root, null, latestRevision);
            
            // Root element must be added by the caller.
            elements.add(root);
            return elements;
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("A subversion failure occured while" +
                " recursing over the directory tree: " + svne, svne);
        }

    }


    // from interface ProjectStorage
    public SwiftlyDocument getDocument (PathElement path)
        throws ProjectStorageException
    {
        SVNRepository svnRepo;
        OutputStream fileOutput;
        File tempFile;
        SwiftlyTextDocument swiftlyDoc;

        try {
            tempFile = File.createTempFile("swiftly-file", ".svnstorage");
            tempFile.deleteOnExit();        
            fileOutput = new FileOutputStream(tempFile);    
        } catch (IOException ioe) {
            throw new ProjectStorageException.InternalError("Failed to create temporary data file" +
                ioe, ioe);
        }

        try {
            svnRepo = getSVNRepository();
            svnRepo.getFile(path.getAbsolutePath(), -1, null, fileOutput);
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("A subversion failure occured while" +
                " fetching the requested document '" + path.getAbsolutePath() + "' : " +
                svne, svne);
        }

        try {
            swiftlyDoc = new SwiftlyTextDocument(new FileInputStream(tempFile), path, ProjectStorage.TEXT_ENCODING);
        } catch (IOException ioe) {
            throw new ProjectStorageException.InternalError("Failure instantiating SwiftlyDocument: " +
                ioe, ioe);            
        } finally {
            tempFile.delete();
        }

        return swiftlyDoc;
    }

    /** Store a document in the repository. */
    public void putDocument (SwiftlyDocument document, String logMessage)
        throws ProjectStorageException
    {
        SVNRepository svnRepo;
        ISVNEditor editor;
        SVNCommitInfo commitInfo;
        long latestRevision;
        PathElement pathElement;
        String filePath;
        String fileName;
        String dirPath;
        boolean newFile = false;

        pathElement = document.getPathElement();

        // Repository-relative paths.
        filePath = pathElement.getAbsolutePath();
        fileName = pathElement.getName();
        dirPath = new File(filePath).getParent();

        // Instantiate a reference to the repository, check if
        // this is a newly added file.
        try {
            svnRepo = getSVNRepository();
            latestRevision = svnRepo.getLatestRevision();

            // Are we adding a new file, or updating an existing one?
            SVNNodeKind nodeKind = svnRepo.checkPath(filePath, latestRevision);
            if (nodeKind == SVNNodeKind.NONE) {
                newFile = true;
            } else if (nodeKind != SVNNodeKind.FILE) {
                throw new ProjectStorageException.InternalError("The subversion root path is not a file");
            }

            // Fire up the commit editor.
            editor = svnRepo.getCommitEditor(logMessage, null);
        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError(
                "Failed to open the storage repository: " + svne, svne);
        }

        // Store the file in the repository
        try {
            SVNDeltaGenerator deltaGenerator;
            String checksum;
            
            // Open the repository root.
            editor.openRoot(-1);           

            // Open the enclosing directory.
            editor.openDir(dirPath, -1);

            // Add/open the file.
            if (newFile) {
                editor.addFile(fileName, null, -1);
            } else {
                editor.openFile(fileName, -1);
            }

            // Set up and stream deltas.
            editor.applyTextDelta(fileName, null);
            deltaGenerator = new SVNDeltaGenerator();

            if (newFile) {
                checksum = deltaGenerator.sendDelta(fileName, document.getModifiedData(), editor, true);                
            } else {
                checksum = deltaGenerator.sendDelta(fileName, document.getOriginalData(), 0, document.getModifiedData(), editor, true);                
            }

            // (Re)set the mime-type.
            editor.changeFileProperty(fileName, SVNProperty.MIME_TYPE, pathElement.getMimeType());

            // Close the file.
            editor.closeFile(fileName, checksum);

            // Close the directory.
            editor.closeDir();

            // Close the repository root.
            editor.closeDir();

            // Commit the whole lot.
            commitInfo = editor.closeEdit();
        } catch (SVNException e) {
            try {
                // We have to abort the open edit. It can also raise an SVNException!
                editor.abortEdit();                
            } catch (SVNException eabort) {
                throw new ProjectStorageException.InternalError("Failure aborting subversion commit: "
                    + eabort, eabort);                
            }
            
            // Report failure.
            throw new ProjectStorageException.InternalError("Failure committing project template: "
                + e, e);
        } catch (IOException ioe) {
            throw new ProjectStorageException.InternalError("Could not add/modify file, failure reading input:" +
                ioe, ioe);
        }

        // Validate the commit.
        if (commitInfo == null) {
            throw new ProjectStorageException.InternalError("Subversion commit failed, null commit info returned");            
        }

        if (commitInfo.getNewRevision() == -1) {
            throw new ProjectStorageException.TransientFailure("Subversion commit failed, file(s) out of date: " + commitInfo.getErrorMessage());
        }
    }

    // from interface ProjectStorage
    public void export (File exportPath)
        throws ProjectStorageException
    {
        SVNRepository svnRepo;
        SVNNodeKind nodeKind;
        ISVNReporterBaton reporterBaton;
        ISVNEditor exportEditor;
        long latestRevision;

        // Fire off an export
        try {
            // Connect to the repository and get the latest revision. This is what we'll export.
            svnRepo = getSVNRepository();
            latestRevision = svnRepo.getLatestRevision();

            // Validate the repository URL
            nodeKind = svnRepo.checkPath("", latestRevision);
            if (nodeKind != SVNNodeKind.DIR) {
                // This really shouldn't happen!
                throw new ProjectStorageException.ConsistencyError("Project subversion URL " +
                    "does not refer to a directory.");
            }

            // Do the actual export
            reporterBaton = new ExportReporterBaton(latestRevision);
            exportEditor = new ExportEditor(exportPath);
            svnRepo.update(latestRevision, null, true, reporterBaton, exportEditor);

        } catch (SVNException svne) {
            throw new ProjectStorageException.InternalError("Project export failed: " + svne, svne);
        }
    }


    /**
     * Given a URI and a project Id, return the project's subversion URL.
     * This is composed of the base server URL + the project ID.
     */
    protected SVNURL getSVNURL ()
        throws SVNException, URISyntaxException
    {
        URI baseURI = _storageRecord.toURI();            
        SVNURL url = SVNURL.parseURIDecoded(baseURI.toString());
        return url.appendPath(Integer.toString(_project.projectId), false);
    }


    /** 
     * Recursively retrieves the entire subversion directory structure.
     */
    private List<PathElement> recurseTree (SVNRepository svnRepo, PathElement parent,
        List<PathElement> result, long revision)
        throws SVNException, ProjectStorageException
    {
        if (result == null) {
            result = new ArrayList<PathElement>();
        }

        @SuppressWarnings("unchecked")
        Collection<SVNDirEntry> entries = (Collection<SVNDirEntry>) svnRepo.getDir(parent.getAbsolutePath(), revision, null, (Collection)null);
        for (SVNDirEntry entry : entries) {
            PathElement node;
            SVNNodeKind kind;
            Map<String,String> properties;
            String mimeType;
            
            kind = entry.getKind();
            if (kind == SVNNodeKind.DIR) {
                node = PathElement.createDirectory(entry.getName(), parent);
                // Recurse
                recurseTree(svnRepo, node, result, revision);
            } else if (kind == SVNNodeKind.FILE) {
                // Fetch the file properties.
                properties = new HashMap<String,String>();
                svnRepo.getFile(parent.getAbsolutePath() + "/" + entry.getName(), revision,
                    properties, null);
                
                // Pull out the mime type.
                mimeType = properties.get(SVNProperty.MIME_TYPE);
                
                // Initialize a new PathElement node.
                node = PathElement.createFile(entry.getName(), parent, mimeType);
            } else {
                throw new ProjectStorageException.InternalError("Received an unhandled subversion node type: " + kind);
            }

            result.add(node);
        }

        return result;
    }


    /**
     * Recursively add a given local directory to a repository edit instance.
     */
    private static void svnAddDirectory (ISVNEditor editor, String parentPath, File sourceDir)
        throws SVNException, FileNotFoundException, IOException
    {
        MimeTypeIdentifier identifier = new MagicMimeTypeIdentifier();

        for (File file : sourceDir.listFiles()) {
            String fileName = file.getName();
            String subPath = parentPath + "/" + fileName;
            File targetFile = new File(sourceDir, fileName);

            if (file.isDirectory()) {

                // Skip .svn directories.
                if (fileName.equals(".svn")) {
                    continue;
                }

                // Add the directory.
                editor.addDir(subPath, null, -1);

                // Recurse
                svnAddDirectory(editor, subPath, targetFile);
                editor.closeDir();
            } else if (file.isFile()) {
                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
                String checksum;
                String mimeType = null;

                /* Identify the mime type */
                FileInputStream stream = new FileInputStream(targetFile);
                byte[] firstBytes = new byte[identifier.getMinArrayLength()];

                // Read identifying bytes from the to-be-added file
                if (stream.read(firstBytes, 0, firstBytes.length) >= firstBytes.length) {
                    // Required data was read, attempt magic identification
                    mimeType = identifier.identify(firstBytes, targetFile.getName(), null);                    
                }
                
                // If that failed, try our internal path-based type detection.
                if (mimeType == null) {
                    // Get the miserly byte mime-type
                    byte miserMimeType = MediaDesc.suffixToMimeType(targetFile.getName());
                    
                    // If a valid type was returned, convert to a string.
                    // Otherwise, don't set a mime type.
                    // TODO: binary file detection
                    if (miserMimeType != -1) {
                        mimeType = MediaDesc.mimeTypeToString(miserMimeType);
                    }
                }

                // Add the file, generating the delta and checksum.
                editor.addFile(subPath, null, -1);
                editor.applyTextDelta(subPath, null);
                checksum = deltaGenerator.sendDelta(subPath, new FileInputStream(targetFile), editor, true);
                
                // Set the mimetype, if any
                if (mimeType != null) {
                    editor.changeFileProperty(subPath, SVNProperty.MIME_TYPE, mimeType);
                }

                // Ship it
                editor.closeFile(subPath, checksum);
            }
        }
    }

    /**
     * Return a re-usable svn repository instance from the pool.
     */
    private SVNRepository getSVNRepository()
        throws SVNException
    {
        return _svnPool.createRepository(_svnURL, true);
    }


    /**
     * SVNKit export reporter. Describes the state of local items --
     * in the case of export, all local items are non-existent.
     */
    protected class ExportReporterBaton
        implements ISVNReporterBaton
    {
        /** Export the given revision. */
        public ExportReporterBaton (long revision)
        {
            _exportRevision = revision;
        }

        public void report (ISVNReporter reporter)
            throws SVNException
        {
            // Set the root path to the export revision, with the startEmpty
            // flag true to inform the reporter that the directory has no
            // entries or properties.
            try {
                reporter.setPath("", null, _exportRevision, true);
                reporter.finishReport();                
            } catch (SVNException svne) {
                reporter.abortReport();
            }
        }

        private long _exportRevision;
    }
    

    /**
     * SVNKit export editor. Responsible for writing provided files
     * and directories to the file system.
     */
     protected class ExportEditor
        implements ISVNEditor
    {
        /**
         * Instantiate the exporter with the given root directory
         * to which all exported files will be relative.
         */
        public ExportEditor (File rootPath)
        {
           _rootPath = rootPath;
           _deltaProcessor = new SVNDeltaProcessor();
        }


        /** Create a directory. */
        public void addDir (String relativeDirPath, String copyFromPath, long copyFromRevision)
            throws SVNException
        {
            File newDir = new File(_rootPath, relativeDirPath);

            if (!newDir.isDirectory() && !newDir.mkdirs()) {
                SVNErrorMessage msg = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Failed to add the directory ''{0}''", newDir);
                throw new SVNException(msg);
            }
        }

        /** Create a file. */
        public void addFile (String relativeFilePath, String copyFromPath, long copyFromRevision)
            throws SVNException
        {
            File newFile = new File(_rootPath, relativeFilePath);
            if (newFile.exists()) {
                SVNErrorMessage msg = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "File ''{0}'' already exists", newFile);
                throw new SVNException(msg);
            }

            try {
                newFile.createNewFile();                
            } catch (IOException ioe) {
                SVNErrorMessage msg = SVNErrorMessage.create(SVNErrorCode.IO_ERROR,
                    "Could not create file ''{0}''", newFile);
                throw new SVNException(msg);
            }
        }

        public void applyTextDelta(String relativeFilePath, String baseChecksum)
            throws SVNException
        {
            File editFile = new File(_rootPath, relativeFilePath);

            // Apply the given delta to the local file.
            _deltaProcessor.applyTextDelta(null, editFile, false);
        }
        
        public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow)
            throws SVNException
        {
            // Prepare for the next diff window.
            return _deltaProcessor.textDeltaChunk(diffWindow);
        }
        
        public void textDeltaEnd(String path)
            throws SVNException
        {
            // Finalize the delta, including computing the checksum (not enabled).
            _deltaProcessor.textDeltaEnd();
        } 

        /** Close the edit, commit any remaining changes. */
        public SVNCommitInfo closeEdit ()
        {
            return null;
        }

        // No implementation necessary on the following. Yay pointless boilerplate!

        /** Target the given revision. */
        public void targetRevision (long revision) throws SVNException {}

        /** Open the project root. */
        public void openRoot (long revision) throws SVNException {}
    
        /** Open the given directory. */
        public void openDir (String path, long revision) throws SVNException {}
        
        /** Change the directory property. */
        public void changeDirProperty (String name, String value) throws SVNException {}
        
        /** Open the given file. */
        public void openFile (String path, long revision) throws SVNException {}
    
        /** Change the file property. */
        public void changeFileProperty (String path, String name, String value) throws SVNException {}

        /** Close the currently open directory. */
        public void closeDir () throws SVNException {}

        /** Close the given file. */
        public void closeFile (String path, String textChecksum) throws SVNException {}
        
        /** Delete the given entry. */
        public void deleteEntry (String path, long revision) throws SVNException {}
        
        /** Absent directory. */
        public void absentDir (String path) throws SVNException {}
        
        /* Absent file. */
        public void absentFile (String path) throws SVNException {}

        /* Abort the export -- an error occured. */
        public void abortEdit () throws SVNException {}

        /* Transforms server deltas to file contents. */
        private SVNDeltaProcessor _deltaProcessor = new SVNDeltaProcessor();

        /** Export root. */
        private File _rootPath;
    }

    /** Reference to the project. */
    protected SwiftlyProject _project;

    /** Reference to the project storage record. */
    protected SwiftlySVNStorageRecord _storageRecord;

    /** Reference to the project storage subversion URL. */
    private SVNURL _svnURL;

    /** 
      * Reference to the project storage's subversion repository pool.
      * Repositories are not re-entrant or thread-safe -- transactional
      * operations (such as an edit), lock all access to the repository instance. The pool
      * provides one repository instance per thread, and as such, one should be careful to
      * open and close all transactions within the same method call. */
    private ISVNRepositoryPool _svnPool;

}
