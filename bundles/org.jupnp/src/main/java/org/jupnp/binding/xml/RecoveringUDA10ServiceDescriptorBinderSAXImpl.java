/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.binding.xml;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Service;

/**
 * Implementation based on JAXP SAX.
 * This binder does not enforce strict UPnP spec conformance - it rather corrects errors in SCPD files
 * or ignores services that are not correctly declared.
 *
 * @author Hans-Jörg Merk - added faulty descriptors as found by Belkin WeMo
 */
public class RecoveringUDA10ServiceDescriptorBinderSAXImpl extends UDA10ServiceDescriptorBinderSAXImpl {

    private Logger log = Logger.getLogger(ServiceDescriptorBinder.class.getName());

    @Override
    public <S extends Service> S describe(S undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        if (descriptorXml == null || descriptorXml.length() == 0) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            log.fine("Reading service from XML descriptor");

            String fixedXml = fixBOM(descriptorXml);
            fixedXml = fixRetval(fixedXml);
            fixedXml = fixQuotes(fixedXml);
            return super.describe(undescribedService, fixedXml);
        } catch (DescriptorBindingException e) {
            log.warning(e.getMessage());
        }
        return null;
    }

    // Some Belkin WeMo SCPD files start with a UTF-8 BOM, which is not UPnP compliant.
    protected String fixBOM(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            descriptorXml = descriptorXml.trim().replaceFirst("^([\\W]+)<", "<");
            String newXml;
            Matcher junkMatcher = (Pattern.compile("^([\\W]+)<")).matcher(descriptorXml.trim());
            newXml = junkMatcher.replaceFirst("<");
            return newXml.replaceAll("\0", " ");

        }
        return descriptorXml;
    }

    // Some Belkin WeMo SCPD files contain invalid "retval" values
    protected String fixRetval(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            if (descriptorXml.contains("<retval")) {
                log.warning("Detected invalid service value 'retval', replacing it");
                descriptorXml = descriptorXml.replaceAll("<retval/>", " ");
                return descriptorXml.replaceAll("<retval />", " ");
            }
        }
        return descriptorXml;
    }

    // Some Belkin WeMo SCPD files contain invalid quotes
    protected String fixQuotes(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            if (descriptorXml.contains("Key\"")) {
                log.warning("Detected invalid quotes, replacing it");
                descriptorXml = descriptorXml.replaceAll("\"smartprivateKey\"", "smartprivateKey");
                return descriptorXml.replaceAll("\"pluginprivateKey\"", "pluginprivateKey");
            }
        }
        return descriptorXml;
    }

}
