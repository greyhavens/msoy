//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.client.util.ClientFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Implements all the nasty vagaries of marshalling and unmarshalling Swiftly XML-RPC requests.
 */
public class SwiftlyClientRpc
{
    public SwiftlyClientRpc (URL rpcURL, String authtoken)
    {
        XmlRpcClientConfigImpl config;
        XmlRpcClient client;
        ClientFactory factory;

        // Cache the authtoken
        _authtoken = authtoken;

        // Set up the client configuration
        config = new XmlRpcClientConfigImpl();
        config.setServerURL(rpcURL);
        config.setEnabledForExtensions(true);

        // Configure a new client
        client = new XmlRpcClient();
        client.setConfig(config);

        // Instantiate our proxies
        factory = new ClientFactory(client);
        _project = (SwiftlyProjectRpc) factory.newInstance(SwiftlyProjectRpc.class);
    }

    public ArrayList<PathElement> getProjects ()
    {
        ArrayList<PathElement> projectList = new ArrayList<PathElement>();
        for (Map<String,Object> struct : _project.getProjects(_authtoken)) {
            PathElement project = PathElement.createRoot(
                (String)struct.get(SwiftlyProjectRpc.PROJECT_NAME));
            projectList.add(project);
        }
        return projectList;
    }

    public PathElement createProject (String projectName)
    {
        _project.createProject(_authtoken, projectName);
        // XXX dummy up a project
        return(PathElement.createRoot(projectName));
    }

    /** Swiftly XML-RPC Connection. */
    protected SwiftlyProjectRpc _project;

    /** Cached authorization token. */
    protected String _authtoken;
}
