/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.tools.verifier.tests.ejb.elements;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.Collection;
import java.util.Map;

/** 
 * Methods used in method permission element of the deployment descriptor
 * must be methods defined in the enterprise bean's component and/or home 
 * interface.
 */
public class MethodPermissionMethodExists extends MethodsExist implements EjbCheck { 

    /** 
     * Methods used in method permission element of the deployment descriptor
     * must be methods defined in the enterprise bean's remote and/or home 
     * interface.
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        if ((descriptor instanceof EjbSessionDescriptor)  ||
                (descriptor instanceof EjbEntityDescriptor)) {
            Map<MethodPermission,
                    Collection<MethodDescriptor>> permissionedMethods = descriptor.getMethodPermissionsFromDD();
        
            if (permissionedMethods!=null) {

                for (MethodPermission methodPermission : permissionedMethods.keySet()) 
                    for (MethodDescriptor methodDescriptor : permissionedMethods.get(methodPermission)) 
                        checkMethodStyles(methodDescriptor, descriptor);
            }
        }

        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "Valid method permission method(s) found."));
        }
        return result;
    }
}

