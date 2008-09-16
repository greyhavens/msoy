//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.google.common.collect.Sets;

import com.samskivert.jdbc.depot.DatabaseException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;

import com.threerings.msoy.server.ServerConfig;

/**
 * A tool for dumping and restoring an entire Whirled database using Serialized PersistentRecord
 * instances as the intermediate format so that one can magically move data between Postgres and
 * MySQL.
 */
public class DatabaseMover
{
    public static void main (String[] args)
    {
        if (args.length < 2) {
            System.err.println(USAGE);
            System.exit(255);

        } else if (args[0].equals("dump")) {
            try {
                new DatabaseMover().dump(new File(args[1]));
            } catch (Exception e) {
                System.err.println("Failed to dump database: " + e);
                e.printStackTrace(System.err);
            }

        } else if (args[0].equals("restore")) {
            try {
                new DatabaseMover().restore(new File(args[1]));
            } catch (Exception e) {
                System.err.println("Failed to restore database: " + e);
                e.printStackTrace(System.err);
            }

        } else {
            System.err.println(USAGE);
            System.exit(255);
        }
    }

    protected DatabaseMover ()
        throws Exception
    {
        _repo = new MoverRepository(
            new PersistenceContext("msoy", ServerConfig.createConnectionProvider()));
    }

    protected void dump (File target)
        throws Exception
    {
        Set<Class<? extends PersistentRecord>> classes = Sets.newHashSet();

        // grind through all the jar files in our classpath looking for anything that looks like a
        // persistent record
        URLClassLoader loader = (URLClassLoader)getClass().getClassLoader();
        for (URL url : loader.getURLs()) {
            JarInputStream jin = new JarInputStream(url.openStream());
            JarEntry entry;
            while ((entry = jin.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith("com/threerings") || !name.endsWith("Record.class")) {
                    continue;
                }

                // resolve the class
                String cname = name.replace("/", ".").replaceAll(".class$", "");
                try {
                    Class<?> clazz = Class.forName(cname);
                    if (!PersistentRecord.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    if (clazz.getAnnotation(Computed.class) != null) {
                        continue;
                    }
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }
                    classes.add(clazz.asSubclass(PersistentRecord.class));

                } catch (Exception e) {
                    System.err.println("Unable to resolve " + cname + ": " + e);
                }
            }
        }

        ObjectOutputStream oout = new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(target)));

        // now go through and slurp down each of these classes in turn
        for (Class<? extends PersistentRecord> clazz : classes) {
            System.out.println("Slurping: " + clazz);
            for (PersistentRecord record : _repo.slurp(clazz)) {
                oout.writeObject(record);
            }
        }

        oout.close();
    }

    protected void restore (File target)
        throws Exception
    {
        ObjectInputStream oin = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(target)));

        Set<Class<? extends PersistentRecord>> classes = Sets.newHashSet();
        try {
            while (true) {
                PersistentRecord record = (PersistentRecord)oin.readObject();
                if (!classes.contains(record.getClass())) {
                    if (_repo.slurp(record.getClass()).size() > 0) {
                        System.err.println("Refusing to restore to a database that has contents: " +
                                           record.getClass());
                        System.exit(255);
                    }
                    classes.add(record.getClass());
                }
                _repo.add(record);
            }
        } catch (EOFException eofe) {
            // done!
        }
        oin.close();
    }

    protected static class MoverRepository extends DepotRepository
    {
        public MoverRepository (PersistenceContext perCtx) {
            super(perCtx);
        }
        public <T extends PersistentRecord> List<T> slurp (Class<T> type) throws DatabaseException {
            return findAll(type);
        }
        public <T extends PersistentRecord> void add (T record) throws DatabaseException {
            insert(record);
        }
        protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes) {
        }
    }

    protected MoverRepository _repo;

    protected static final String USAGE = "Usage: DatabaseMover [dump|restore] dbdata.dat";
}
