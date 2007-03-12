//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc. Please do not redistribute.

package com.whirled.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An ant task that generates a new game project from a template file and some user input.
 */
public class NewProjectTask extends Task
{
    /**
     * Tells the task where to find its template files.
     */
    public void setTemplates (File templates)
    {
        _templates = templates;
    }

    /**
     * Sets the type of thing we're making.
     */
    public void setType (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public void execute () throws BuildException
    {
        // first we need to ask the user for some information
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        File sdkdir;
        while (true) {
            sdkdir = new File(readInput(input, "Where is your Flex SDK installed?"));
            File mxmlcjar = new File(sdkdir, "lib" + File.separator + "mxmlc.jar");
            if (mxmlcjar.exists()) {
                break;
            } else {
                System.err.println(mxmlcjar + " could not be found. " +
                                   "Be sure you provided the correct path.");
            }
        }

        String project;
        do {
            System.out.println("Please enter the name of your " + _type + " project.");
            project = readInput(input, "For example, Best" + _type + "Ever:");

            System.out.println("Your main class will be called:");
            System.out.println("  " + project + ".as");

        } while (!readConfirm(input));

        // create a directory for the project
        File pdir = new File(project);
        makeDir(pdir);

        // customize the template files and copy them into the right place
        HashMap<String, String> subs = new HashMap<String, String>();
        subs.put("flexpath", sdkdir.getAbsolutePath());
        subs.put("project", project);

        copyFile(input, new File(_templates, "whirled-config.xml"),
                 new File(pdir, "whirled-config.xml"), subs);
        copyFile(input, new File(_templates, "build.xml"), new File(pdir, "build.xml"), subs);
        copyFile(input, new File(_templates, _type + ".as"), new File(pdir, project + ".as"), subs);

        System.out.println("Done! Your new project has been created in '" + pdir + "'.");
        System.out.println("");
        System.out.println("Go into that directory and try building it like so:");
        System.out.println("  cd " + pdir);
        System.out.println("  ant compile");
        System.out.println("");
        System.out.println("Have fun making your new " + _type + ".");
    }

    protected String readInput (BufferedReader input, String prompt)
        throws BuildException
    {
        String line;
        try {
            do {
                System.out.print(prompt);
                line = input.readLine();
                if (line == null) { // handle EOF
                    throw new BuildException("Aborting");
                }

            } while (line.length() == 0);
            return line;

        } catch (IOException ioe) {
            throw new BuildException("Error reading input: " + ioe);
        }
    }

    protected boolean readConfirm (BufferedReader input)
        throws BuildException
    {
        return readInput(input, "Is this OK? [y/n]").equalsIgnoreCase("y");
    }

    protected void makeDir (File dir)
        throws BuildException
    {
        if (!(dir.exists() && dir.isDirectory())) {
            if (!dir.mkdirs()) {
                throw new BuildException("Failed to create directory '" + dir + "'.");
            }
        }
    }

    protected void copyFile (
        BufferedReader input, File source, File dest,
        HashMap<String, String> subs)
    {
        // ask whether to overwrite if the file already exists
        if (dest.exists()) {
            if (!_overwriteAll) {
                String response = readInput(
                    input, "File '" + dest + "' already exists. Overwrite? [y/n/A]");
                if (response.equalsIgnoreCase("y")) {
                    // fall through and overwrite
                } else if (response.equals("A")) {
                    _overwriteAll = true;
                    // fall through and overwrite
                } else {
                    System.out.println("Leaving '" + dest + "' as is.");
                    return;
                }
            }
            System.out.println("  Overwriting '" + dest + "'.");
        } else {
            System.out.println("  Creating '" + dest + "'.");
        }

        try {
            BufferedReader in = new BufferedReader(new FileReader(source));
            FileWriter out = new FileWriter(dest);
            String line;
            StringBuffer sline = new StringBuffer();
            while ((line = in.readLine()) != null) {
                Matcher m = _subre.matcher(line);
                while (m.find()) {
                    // convert single backslashes to double backslashes,
                    // otherwise appendReplacement will interpret them as escape sequences.
                    String replacement = subs.get(m.group(1)).replaceAll("\\\\", "\\\\\\\\");
                    m.appendReplacement(sline, replacement);
                }
                m.appendTail(sline);
                out.write(sline.toString());
                sline.setLength(0);
                out.write(LINE_SEP);
            }
            out.close();
            in.close();

        } catch (IOException ioe) {
            throw new BuildException("Failed to create '" + dest + "': " + ioe);
        }
    }

    protected String _type;
    protected File _templates;
    protected boolean _overwriteAll;
    protected Pattern _subre = Pattern.compile("@([A-Za-z0-9]+)@");

    protected static final String LINE_SEP = System.getProperty("line.separator");
}
