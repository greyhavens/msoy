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

import flex.ant.config.ConfigBoolean;
import flex.ant.config.ConfigString;
import flex.ant.config.ConfigVariable;
import flex.ant.config.NestedAttributeElement;
import flex.ant.config.OptionSpec;
import flex.ant.config.OptionSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.types.Commandline;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public final class Fonts implements OptionSource, DynamicConfigurator
{
    /*
     * Use this defintion of lrSpec if you want to allow users to set the
     * compiler.fonts.languages.language-range by using a nested element named
     * languages.language-range:
     *
     * private static OptionSpec lrSpec = new OptionSpec("compiler.fonts.languages.language-range", "languages.language-range");
     *
     * Note that using this will no longer allow users to set the option by
     * using a language-range nested element.
     */
    private static OptionSpec lrSpec = new OptionSpec("compiler.fonts.languages", "language-range");
    private static OptionSpec maSpec = new OptionSpec("compiler.fonts", "managers");

    private final ConfigVariable[] attribs;

    private final ArrayList nestedAttribs;

    public Fonts()
    {
        attribs = new ConfigVariable[] {
            new ConfigBoolean(new OptionSpec("compiler.fonts", "flash-type")),
            new ConfigBoolean(new OptionSpec("compiler.fonts", "advanced-anti-aliasing")),
            new ConfigString(new OptionSpec("compiler.fonts", "local-fonts-snapshot")),
            new ConfigString(new OptionSpec("compiler.fonts", "max-cached-fonts")),
            new ConfigString(new OptionSpec("compiler.fonts", "max-glyphs-per-face"))
        };

        nestedAttribs = new ArrayList();
    }

    /*=======================================================================*
     *  Attributes                                                           *
     *=======================================================================*/

    public void setDynamicAttribute(String name, String value)
    {
        ConfigVariable var = null;

        for (int i = 0; i < attribs.length && var == null; i++) {
            if (attribs[i].matches(name))
                var = attribs[i];
        }

        if (var != null)
            var.set(value);
        else
            throw new BuildException("The <font> type doesn't support the \""
                                     + name + "\" attribute.");
    }

    /*=======================================================================*
     *  Nested Elements                                                      *
     *=======================================================================*/

    public Object createDynamicElement(String name)
    {
        if (lrSpec.matches(name)) {
            NestedAttributeElement e = new NestedAttributeElement(new String[] { "lang", "range" }, lrSpec);
            nestedAttribs.add(e);
            return e;
        }
        else {
            throw new BuildException("Invalid element: " + name);
        }
    }

    public NestedAttributeElement createManager()
    {
        NestedAttributeElement e = new NestedAttributeElement("class", maSpec);
        nestedAttribs.add(e);
        return e;
    }

    /*=======================================================================*
     *  OptionSource interface                                               *
     *=======================================================================*/

    public void addToCommandline(Commandline cmdl)
    {
        for (int i = 0; i < attribs.length; i++)
            attribs[i].addToCommandline(cmdl);

        Iterator it = nestedAttribs.iterator();

        while (it.hasNext())
            ((OptionSource) it.next()).addToCommandline(cmdl);
    }

} //End of Fonts
