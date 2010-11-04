//
// $Id: $

package com.threerings.msoy.web.server;

import java.util.Collections;

import com.google.common.base.Joiner;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.ServerConfig;

public abstract class CloudfrontTool
{
   public static final void main (String[] args)
    {
        OriginAccessIdentityAPI oConn =
            new OriginAccessIdentityAPI(ServerConfig.cloudId, ServerConfig.cloudKey);
        InvalidationAPI iConn =
            new InvalidationAPI(ServerConfig.cloudId, ServerConfig.cloudKey);
        DistributionAPI dConn =
            new DistributionAPI(ServerConfig.cloudId, ServerConfig.cloudKey);

        try {
            Object result = null;

            if (args.length > 0) {
                String cmd = args[0];
                if ("dists".equals(cmd)) {
                    result = dConn.getDistributions();
                } else if ("oaids".equals(cmd)) {
                    result = oConn.getOriginAccessIdentities();
                }
                if (args.length > 1) {
                    if ("invreqs".equals(cmd)) {
                        result = iConn.getInvalidations(args[1]);
                    } else if ("dist".equals(cmd)) {
                        result = dConn.getDistribution(args[1]);
                    } else if ("distconf".equals(cmd)) {
                        result = dConn.getDistributionConfig(args[1]);
                    } else if ("oaid".equals(cmd)) {
                        result = oConn.getOriginAccessIdentity(args[1]);
                    } else if ("oaidconf".equals(cmd)) {
                        result = oConn.getOriginAccessIdentityConfig(args[1]);
                    }
                }
                if (args.length > 2) {
                    if ("invalidate".equals(cmd)) {
                        result = iConn.invalidateObjects(args[1], Collections.singleton(args[2]));
                    } else if ("invreq".equals(cmd)) {
                        result = iConn.getInvalidation(args[1], args[2]);
                    }
                }
            }

            if (result instanceof Iterable) {
                result = Joiner.on("\n").join((Iterable) result);

            } else if (result == null) {
                System.err.println(
                    "Available commands:\n" +
                    "dists\n" +
                    "dist <distId>\n" +
                    "distconf <distId>\n" +
                    "oaids\n" +
                    "oaid <id>\n" +
                    "oaidconf <id>\n" +
                    "invreqs\n" +
                    "invreq <batchId>\n" +
                    "invalidate <distId> <key>");
                return;
            }
            System.out.println("Result: " + StringUtil.toString(result));

        } catch (CloudfrontException e) {
            e.printStackTrace();
        }
    }
}
