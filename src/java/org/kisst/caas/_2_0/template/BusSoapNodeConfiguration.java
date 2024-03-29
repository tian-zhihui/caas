//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.21 at 10:05:41 AM CEST 
//


package org.kisst.caas._2_0.template;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * Holds the configuration XML for the service group. This is a 1-to-1 copy of the value in LDAP. The format is
 *         a Cordys internal format which looks like this: <configuration xmlns=""> <routing
 *         ui_algorithm="failover" ui_type="loadbalancing"> <numprocessors>100000</numprocessors>
 *         <algorithm>com.eibus.transport.routing.DynamicRouting</algorithm> </routing> <validation>
 *         <protocol>false</protocol> <payload>true</payload> </validation>
 *         <IgnoreWhiteSpaces>false</IgnoreWhiteSpaces> </configuration>
 * 
 * <p>Java class for BusSoapNodeConfiguration complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BusSoapNodeConfiguration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='skip'/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BusSoapNodeConfiguration", propOrder = {
    "any"
})
public class BusSoapNodeConfiguration {

    @XmlAnyElement
    protected Element any;

    /**
     * Gets the value of the any property.
     * 
     * @return
     *     possible object is
     *     {@link Element }
     *     
     */
    public Element getAny() {
        return any;
    }

    /**
     * Sets the value of the any property.
     * 
     * @param value
     *     allowed object is
     *     {@link Element }
     *     
     */
    public void setAny(Element value) {
        this.any = value;
    }

}
