package com.threerings.msoy.swiftly.server.storage;

import java.io.File;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Re-usable static methods for testing ProjectStorage implementations.
 * @author landonf
 *
 */
public class ProjectStorageUnitTest
{
    /** Static, brittle path to the test template. Sorry. */
    public static final File TEMPLATE_DIR = new File("data/swiftly/templates/unittest");

    public static final File GAME_TEMPLATE_DIR = new File("data/swiftly/templates/game");

    /** Mock up a project record. */
    public static SwiftlyProject mockProject ()
    {
        SwiftlyProject project;

        // Mock up a project record.
        project = new SwiftlyProject();
        project.projectName = "project-name";
        project.ownerId = 0;
        project.projectType = Item.GAME;

        return project;
    }

}
