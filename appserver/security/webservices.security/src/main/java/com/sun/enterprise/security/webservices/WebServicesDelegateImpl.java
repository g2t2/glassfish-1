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

package com.sun.enterprise.security.webservices;

import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.security.jauth.AuthParam;
import com.sun.enterprise.security.jmac.WebServicesDelegate;
import com.sun.enterprise.security.jmac.config.ConfigHelper.AuthConfigRegistrationWrapper;
import com.sun.enterprise.security.jmac.provider.PacketMessageInfo;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.enterprise.security.jmac.provider.SOAPAuthParam;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.message.MessageInfo;
import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.glassfish.api.invocation.ComponentInvocation;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 *
 * @author kumar.jayanti
 */
@Service
@Singleton
public class WebServicesDelegateImpl implements WebServicesDelegate {

    protected static final Logger _logger = LogUtils.getLogger();

    private static final String DEFAULT_WEBSERVICES_PROVIDER=
            "com.sun.xml.wss.provider.wsit.WSITAuthConfigProvider";
    
     public MessageSecurityBindingDescriptor getBinding(ServiceReferenceDescriptor svcRef, Map properties) {
        MessageSecurityBindingDescriptor binding = null;
        WSDLPort p = (WSDLPort) properties.get("WSDL_MODEL");
        QName portName = null;
        if (p != null) {
            portName = p.getName();
        }
        if (portName != null) {
            ServiceRefPortInfo i = svcRef.getPortInfoByPort(portName);
            if (i != null) {
                binding = i.getMessageSecurityBinding();
            }
        }
        return binding;
    }

    public void removeListener(AuthConfigRegistrationWrapper listener) {
        //TODO:V3 convert the pipes to Tubes.
        ClientPipeCloser.getInstance().removeListenerWrapper(listener);
    }

    public String getDefaultWebServicesProvider() {
        return DEFAULT_WEBSERVICES_PROVIDER;
    }

    public String getAuthContextID(MessageInfo messageInfo) {

        // make this more efficient by operating on packet 
        String rvalue = null;
        if (messageInfo instanceof PacketMessageInfo) {
            PacketMessageInfo pmi = (PacketMessageInfo) messageInfo;
            Packet p = (Packet) pmi.getRequestPacket();
            if (p != null) {
                Message m = p.getMessage();
                if (m != null) {
                    WSDLPort port =
                            (WSDLPort) messageInfo.getMap().get("WSDL_MODEL");
                    if (port != null) {
                        WSDLBoundOperation w = m.getOperation(port);
                        if (w != null) {
                            QName n = w.getName();
                            if (n != null) {
                                rvalue = n.getLocalPart();
                            }
                        }
                    }
                }
            }
            return rvalue;
        } else {
            // make this more efficient by operating on packet 
            return getOpName((SOAPMessage) messageInfo.getRequestMessage());
        }

    }

    public AuthParam newSOAPAuthParam(MessageInfo messageInfo) {
        return new SOAPAuthParam((SOAPMessage)
                                  messageInfo.getRequestMessage(),
			         (SOAPMessage)
			          messageInfo.getResponseMessage());

    }

    private String getOpName(SOAPMessage message) {
        if (message == null) {
            return null;
        }

        String rvalue = null;

        // first look for a SOAPAction header. 
        // this is what .net uses to identify the operation

        MimeHeaders headers = message.getMimeHeaders();
        if (headers != null) {
            String[] actions = headers.getHeader("SOAPAction");
            if (actions != null && actions.length > 0) {
                rvalue = actions[0];
                if (rvalue != null && rvalue.equals("\"\"")) {
                    rvalue = null;
                }
            }
        }

        // if that doesn't work then we default to trying the name
        // of the first child element of the SOAP envelope.

        if (rvalue == null) {
            Name name = getName(message);
            if (name != null) {
                rvalue = name.getLocalName();
            }
        }

        return rvalue;
    }

    private Name getName(SOAPMessage message) {
        Name rvalue = null;
        SOAPPart soap = message.getSOAPPart();
        if (soap != null) {
            try {
                SOAPEnvelope envelope = soap.getEnvelope();
                if (envelope != null) {
                    SOAPBody body = envelope.getBody();
                    if (body != null) {
                        Iterator it = body.getChildElements();
                        while (it.hasNext()) {
                            Object o = it.next();
                            if (o instanceof SOAPElement) {
                                rvalue = ((SOAPElement) o).getElementName();
                                break;
                            }
                        }
                    }
                }
            } catch (SOAPException se) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "WSS: Unable to get SOAP envelope",
                            se);
                }
            }
        }
        return rvalue;
    }

    public Object getSOAPMessage(ComponentInvocation inv) {
        /*V3 commented getting this from EJBPolicyContextDelegate instead
         * currently getting this from EjbPolicyContextDelegate which might be OK
        SOAPMessage soapMessage = null;
	    MessageContext msgContext = inv.messageContext;

            if (msgContext != null) {
                if (msgContext instanceof SOAPMessageContext) {
		    SOAPMessageContext smc =
                            (SOAPMessageContext) msgContext;
		    soapMessage = smc.getMessage();
                }
	    } else {
                soapMessage = inv.getSOAPMessage();
            }

	    return soapMessage;*/
        return null; 
    }
	
}
