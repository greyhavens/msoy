/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package flex.ant.types;

import flex.ant.config.ConfigString;
import flex.ant.config.ConfigVariable;
import flex.ant.config.NestedAttributeElement;
import flex.ant.config.OptionSource;
import flex.ant.config.OptionSpec;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.types.Commandline;

/**
 *
 */
public final class RuntimeSharedLibraryPath implements OptionSource, DynamicConfigurator
{
    private static final String RUNTIME_SHARED_LIBRARY_PATH = "-runtime-shared-library-path";
    private static final String PATH_ELEMENT = "path-element";

    private static OptionSpec urlSpec = new OptionSpec("url");

    private String pathElement;
    private ArrayList urlElements = new ArrayList();

    public RuntimeSharedLibraryPath()
    {
    }

    public void addToCommandline(Commandline commandLine)
    {
        commandLine.createArgument().setValue(RUNTIME_SHARED_LIBRARY_PATH);
        commandLine.createArgument().setValue(pathElement);

        Iterator it = urlElements.iterator();

        while (it.hasNext())
        {
            ((OptionSource) it.next()).addToCommandline(commandLine);
        }
    }

    public Object createDynamicElement(String name)
    {
        URLElement result;

        if (urlSpec.matches(name))
        {
            result = new URLElement();
            urlElements.add(result);
        }
        else
        {
            throw new BuildException("Invalid element: " + name);
        }

        return result;
    }

    public void setDynamicAttribute(String name, String value)
    {
        if (name.equals(PATH_ELEMENT))
        {
            pathElement = value;
        }
        else
        {
            throw new BuildException("The <rutime-shared-library-path> type doesn't support the \"" +
                                     name + "\" attribute.");
        }
    }
}
