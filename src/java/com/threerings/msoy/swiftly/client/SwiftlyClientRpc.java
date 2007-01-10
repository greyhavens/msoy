package com.threerings.msoy.swiftly.client;

import java.net.URL;
import java.util.ArrayList;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Implements all the nasty vagaries of marshalling and unmarshalling Swiftly XML-RPC requests.
 */
public class SwiftlyClientRpc
{
    public SwiftlyClientRpc (URL rpcURL, String authtoken)
    {
        _authtoken = authtoken;

        XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();

        xmlRpcConfig.setServerURL(rpcURL);
        xmlRpcConfig.setEnabledForExtensions(true);

        _xmlrpc = new XmlRpcClient();
        _xmlrpc.setConfig(xmlRpcConfig);
    }

    public ArrayList<SwiftlyProject> getProjects () {
        Object[] result;
        Object[] arguments = { _authtoken };
        ArrayList<SwiftlyProject> projectList = new ArrayList<SwiftlyProject>();

        // TODO -- load the files for the project
        ArrayList<SwiftlyDocument> fileList = new ArrayList<SwiftlyDocument>();
        fileList.add(new SwiftlyDocument("file #1", "Example text"));
        fileList.add(new SwiftlyDocument("file #2", "Example text more"));

        try {
            result = (Object[]) _xmlrpc.execute(RPC_GET_PROJECTS,
                arguments);
            for (Object projectName : result) {
                SwiftlyProject project = new SwiftlyProject((String)projectName, fileList);
                projectList.add(project);
            }
        } catch (Exception e) {
            System.err.println("There was a problem: " + e);
            e.printStackTrace();
        }

        return projectList;
    }

    /** Swiftly XML-RPC Connection. */
    protected XmlRpcClient _xmlrpc;

    /** Cached authorization token. */
    protected String _authtoken;

    // RPC Methods
    /** Get a list of projects. */
    protected static final String RPC_GET_PROJECTS = "project.getProjects";
}
