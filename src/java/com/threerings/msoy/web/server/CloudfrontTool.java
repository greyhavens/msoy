//
// $Id: $

package com.threerings.msoy.web.server;

import java.util.Collections;

import com.google.common.base.Joiner;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.server.CloudfrontConnection.Tagged;
import com.threerings.msoy.web.server.DistributionAPI.Distribution;
import com.threerings.msoy.web.server.DistributionAPI.DistributionConfig;
import com.threerings.msoy.web.server.DistributionAPI.Signer;

import static com.threerings.msoy.Log.log;

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
        CloudfrontURLSigner tool = new CloudfrontURLSigner(
            ServerConfig.cloudSigningKeyId, ServerConfig.cloudSigningKey);

        try {
            Object result = null;

            if (args.length > 0) {
                String cmd = args[0];
                if ("dists".equals(cmd)) {
                    result = dConn.getDistributions();
                } else if ("oaids".equals(cmd)) {
                    result = oConn.getOriginAccessIdentities();
                } else if ("validate".equals(cmd)) {
                    result = validateDistributionForSigning(
                        ServerConfig.cloudDistribution, ServerConfig.cloudSigningKeyId);
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
                    } else if ("selfsign".equals(cmd)) {
                        result = setSelfSigning(args[1], true);
                    }
                }
                if (args.length > 2) {
                    if ("invalidate".equals(cmd)) {
                        result = iConn.invalidateObjects(args[1], Collections.singleton(args[2]));
                    } else if ("invreq".equals(cmd)) {
                        result = iConn.getInvalidation(args[1], args[2]);

                    } else if ("sign".equals(cmd)) {
                        String url = args[1];
                        int days = new Integer(args[2]);
                        int now = ((int) (System.currentTimeMillis() / 1000));
                        log.info("Signing URL for expiration", "days", days, "URL", url);
                        result = tool.signURL(url, now + days * 3600 * 24);
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
                    "invalidate <distId> <key>\n" +
                    "selfsign <distId>\n" +
                    "validate\n" +
                    "sign <url> <days before expiration>");
                return;
            }
            System.out.println("Result: " + StringUtil.toString(result));

        } catch (CloudfrontException e) {
            e.printStackTrace();
        }
    }

    public static Distribution setSelfSigning (String distId, boolean value)
        throws CloudfrontException
    {
        DistributionAPI dConn =
            new DistributionAPI(ServerConfig.cloudId, ServerConfig.cloudKey);
        Tagged<DistributionConfig> tagged = dConn.getDistributionConfig(distId);
        log.info("Fetched distribution", "config", tagged);
        if (tagged.result.selfIsSigner == value) {
            return null;
        }
        tagged.result.selfIsSigner = value;
        return dConn.putConfig(distId, tagged);
    }

    public static String validateDistributionForSigning (String distId, String signingKeyId)
    {
        DistributionAPI dConn =
            new DistributionAPI(ServerConfig.cloudId, ServerConfig.cloudKey);
        try {
            Distribution distribution = dConn.getDistribution(distId);
            log.info("Validating for URL signing", "dist", distribution);

            // first see if the distribution's configuration allows self-signing at all
            if (!distribution.config.selfIsSigner) {
                return "Our CloudFront distribution doesn't allow self-signing.";
            }

            boolean found = false;
            for (Signer signer : distribution.activeTrustedSigners) {
                // we're only interested in self-signing
                if (signer.isSelf) {
                    // there *are* active self-signing keys, do we have the right one?
                    if (!signer.keyIds.contains(signingKeyId)) {
                        return "Our CloudFront distribution has active trusted self-signing keys," +
                            "but our configured cloud_signing_key_id is not among them.";
                    }
                    found = true;
                }
            }
            if (!found) {
                return "Our Cloudfront distribution allows self-signing, but there are no " +
                    "active signing keys available on the account.";
            }
            return null;

        } catch (CloudfrontException e) {
            // warn of the problem
            log.warning("Failed to validate distribution for signing!", e);

            // but don't return an error condition, the URLs might still work
            return null;
        }
    }
}
