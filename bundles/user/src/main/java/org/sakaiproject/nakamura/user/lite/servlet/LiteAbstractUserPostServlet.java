/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.user.lite.servlet;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Base class for servlets manipulating users
 */
@Component(immediate=true, metatype=true,componentAbstract=true)
public abstract class LiteAbstractUserPostServlet extends
        LiteAbstractAuthorizablePostServlet {
    private static final long serialVersionUID = -8401210711297654453L;

    /**
     * To be used for the encryption. E.g. for passwords in
     * {@link javax.jcr.SimpleCredentials#getPassword()} SimpleCredentials}
     *
     */
    @Property(value={"sha1"})
    private static final String PROP_PASSWORD_DIGEST_ALGORITHM = "password.digest.algorithm";

    private static final String DEFAULT_PASSWORD_DIGEST_ALGORITHM = "sha1";

    private String passwordDigestAlgoritm = null;

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        super.activate(context);

        Dictionary<?, ?> props = context.getProperties();

        passwordDigestAlgoritm = OsgiUtil.toString(props.get(PROP_PASSWORD_DIGEST_ALGORITHM), DEFAULT_PASSWORD_DIGEST_ALGORITHM);
    }

    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        passwordDigestAlgoritm = null;
    }

    /**
     * Digest the given password using the configured digest algorithm
     *
     * @param pwd the value to digest
     * @return the digested value
     * @throws IllegalArgumentException
     */
    protected String digestPassword(String pwd) throws IllegalArgumentException {
        return digestPassword(pwd, passwordDigestAlgoritm);
    }

    /**
     * Digest the given password using the given digest algorithm
     *
     * @param pwd the value to digest
     * @param digest the digest algorithm to use for digesting
     * @return the digested value
     * @throws IllegalArgumentException
     */
    protected String digestPassword(String pwd, String digest) throws IllegalArgumentException {
        try {
            StringBuffer password = new StringBuffer();
            password.append("{").append(digest).append("}");
            password.append(Text.digest(digest, pwd.getBytes("UTF-8")));
            return password.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

}
