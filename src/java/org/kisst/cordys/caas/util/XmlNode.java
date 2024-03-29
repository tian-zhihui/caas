/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class XmlNode
{
    private final Element element;

    public XmlNode(Element element)
    {
        this.element = element;
    }

    public XmlNode(String name, String namespace)
    {
        this.element = new Element(name, Namespace.getNamespace(namespace));
    }

    public XmlNode(String xml)
    {
        if (xml.indexOf("<") < 0)
        {
            // xml is just an element name
            this.element = new Element(xml);
            return;
        }
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try
        {
            doc = builder.build(new StringReader(xml));
        }
        catch (JDOMException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        this.element = doc.getRootElement();

    }

    @Override
    public XmlNode clone()
    {
        return new XmlNode(this.element.clone());
    }

    public String getName()
    {
        return element.getName();
    }

    public String getNamespace()
    {
        return element.getNamespace().getURI();
    }

    public String getText()
    {
        return element.getText();
    }

    public String getAttribute(String name)
    {
        return getAttribute(name, null);
    }
    
    public Map<String, String> getAttributes()
    {
        Map<String, String> retVal = new LinkedHashMap<String, String>();
        
        List<Attribute> tmp = element.getAttributes();
        for (Attribute a : tmp)
        {
            retVal.put(a.getName(), a.getValue());
        }
        
        return retVal;
    }

    public String getAttribute(String name, String defaultValue)
    {
        Attribute attr = element.getAttribute(name);
        if (attr == null)
            return defaultValue;
        else
            return attr.getValue();
    }
    
    public void removeAttribute(String name)
    {
        element.removeAttribute(name);
    }

    public XmlNode getChild(String path)
    {
        return (XmlNode) get(path);
    }

    public XmlNode getChildWithCreate(String path)
    {
        return (XmlNode) getWithCreate(path);
    }

    
    public XmlNode getParent()
    {
        return new XmlNode(element.getParentElement());
    }

    public Object propertyMissing(String name)
    {
        return get(name);
    }

    public String getChildText(String path)
    {
        Object child = get(path);
        if (child == null)
            return null;
        if (child instanceof String)
            return (String) child;
        return ((XmlNode) child).getText();
    }

    public void setChildText(String path, String value)
    {
        if (path.indexOf("@") >= 0)
            throw new RuntimeException("changing an attribute not yet supported in path " + path);
        Object child = get(path);
        ((XmlNode) child).setText(value);
    }

    public List<XmlNode> getChildren()
    {
        List<?> l = element.getChildren();
        ArrayList<XmlNode> result = new ArrayList<XmlNode>(l.size());
        for (Object o : l)
            result.add(new XmlNode((Element) o));
        return result;
    }

    public List<XmlNode> getChildren(String name)
    {
        List<?> l = element.getChildren(name, null);
        ArrayList<XmlNode> result = new ArrayList<XmlNode>(l.size());
        for (Object o : l)
            result.add(new XmlNode((Element) o));
        return result;
    }

    public Object get(String path)
    {
        String[] parts = path.split("/");
        Element e = element;
        for (String part : parts)
        {
            if (part.equals(".."))
                e = e.getParentElement();
            else if (part.equals("text()"))
                return e.getText();
            else if (part.startsWith("@"))
                return e.getAttribute(part.substring(1)).getValue();
            else
                e = e.getChild(part, null);
            if (e == null)
                return null;
        }
        return new XmlNode(e);
    }
    
    public Object getWithCreate(String path)
    {
        String[] parts = path.split("/");
        Element e = element;
        for (String part : parts)
        {
            if (part.equals(".."))
                e = e.getParentElement();
            else if (part.equals("text()"))
                return e.getText();
            else if (part.startsWith("@"))
                return e.getAttribute(part.substring(1)).getValue();
            else {
                Element child = e.getChild(part, null);
                if (child == null) {
                    child = new Element(part, e.getNamespace());
                    e.addContent(child);
                }
                e=child;
            }
        }
        return new XmlNode(e);
    }

    public String compact()
    {
        XMLOutputter out = new XMLOutputter(Format.getCompactFormat());
        return out.outputString(element);
    }

    @Override
    public String toString()
    {
        XMLOutputter out = new XMLOutputter();
        return out.outputString(element);
    }

    public String shortString(int maxlen)
    {
        String xml = toString();
        if (xml.length() <= maxlen)
            return xml;
        return "<" + getName() + ">...<" + getName() + ">";
    }

    public XmlNode setAttribute(String name, String value)
    {
        element.setAttribute(name, value);
        return this;
    }

    public XmlNode add(String name, String namespace)
    {
        Element child = new Element(name, element.getNamespace());
        element.addContent(child);
        return new XmlNode(child);
    }

    public XmlNode add(String name)
    {
        Element child = new Element(name, element.getNamespace());
        element.addContent(child);
        return new XmlNode(child);
    }

    public XmlNode detach()
    {
        this.element.detach();
        return this;
    }

    public XmlNode setText(String text)
    {
        element.setText(text);
        
        return this;
    }

    public XmlNode add(XmlNode node)
    {
        element.addContent(node.element);
        
        return this;
    }

    public XmlNode remove(XmlNode e)
    {
        element.getChildren().remove(e.element);
        
        return this;
    }

    public String getPretty()
    {
        XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat());
        String xml = out.outputString(element);
        return xml;
    }

    public void save(String filename)
    {
        FileUtil.saveString(new File(filename), getPretty());
    }

    public List<String> diff(XmlNode other)
    {
        ArrayList<String> result = new ArrayList<String>();
        diff(result, getName(), other);
        return result;
    }

    private void diff(List<String> result, String prefix, XmlNode other)
    {
        if (getText() != null && !getText().equals(other.getText()))
        {
            result.add("< node " + prefix + "." + getName() + " has value " + getText());
            result.add("> node " + prefix + "." + other.getName() + " has value " + other.getText());
        }
        List<XmlNode> children1 = getChildren();
        List<XmlNode> children2 = other.getChildren();
        Comparator<XmlNode> comp = new Comparator<XmlNode>() {
            public int compare(XmlNode o1, XmlNode o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        };
        Collections.sort(children1, comp);
        Collections.sort(children2, comp);
        int i1 = 0;
        int i2 = 0;
        while (i1 < children1.size() && i2 < children2.size())
        {
            XmlNode n1 = children1.get(i1);
            XmlNode n2 = children2.get(i2);
            if (n1.getName().equals(n2.getName()))
            {
                n1.diff(result, prefix + "." + n1.getName(), n2);
                i1++;
                i2++;
            }
            else if (n1.getName().compareTo(n2.getName()) < 0)
            {
                result.add("< has node " + prefix + "." + n1.getName());
                i1++;
            }
            else
            {
                result.add("> has node " + prefix + "." + n2.getName());
                i2++;
            }
        }
        while (i1 < children1.size())
        {
            result.add(" < has node " + prefix + "." + children1.get(i1).getName());
            i1++;
        }
        while (i2 < children2.size())
        {
            result.add(" < has node " + prefix + "." + children2.get(i2).getName());
            i2++;
        }
    }

    /**
     * This method executes the XPath on the current element.
     * 
     * @param xpath The XPath to execute.
     * @return The list of elements that match the given XPath.
     */
    public XmlNode xpathSingle(String xpath)
    {
        return xpathSingle(xpath, null);
    }

    /**
     * This method executes the XPath on the current element.
     * 
     * @param xpath The XPath to execute.
     * @return The list of elements that match the given XPath.
     */
    public XmlNode xpathSingle(String xpath, String[][] namespaces)
    {
        List<XmlNode> tmp = xpath(xpath, namespaces);

        if (tmp.size() > 0)
        {
            return tmp.get(0);
        }

        return null;
    }

    /**
     * This method executes the XPath on the current element.
     * 
     * @param xpath The XPath to execute.
     * @return The list of elements that match the given XPath.
     */
    public List<XmlNode> xpath(String xpath)
    {
        return xpath(xpath, null);
    }

    /**
     * This method executes the XPath on the current element.
     * 
     * @param xpath The XPath to execute.
     * @return The list of elements that match the given XPath.
     */
    public List<XmlNode> xpath(String xpath, String[][] namespaces)
    {
        List<XmlNode> retVal = new ArrayList<XmlNode>();

        try
        {
            // Parse the name spaces
            List<Namespace> ns = new ArrayList<Namespace>();

            if (namespaces != null)
            {
                for (String[] nsDetails : namespaces)
                {
                    ns.add(Namespace.getNamespace(nsDetails[0], nsDetails[1]));
                }
            }

            // Create the XPath
            XPathExpression<Element> p = XPathFactory.instance().compile(xpath, Filters.element(), null, ns);

            // Execute the xpath
            List<Element> nodes = p.evaluate(element);

            // Build up the response
            for (Object node : nodes)
            {
                retVal.add(new XmlNode((Element) node));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return retVal;
    }

    /**
     * This method sets the namespace of this element.
     * 
     * @param namespace The new namespace for this element.
     */
    public void setNamespace(String namespace)
    {
        if (StringUtil.isEmptyOrNull(namespace))
        {
            // Remove the namespace
            element.removeNamespaceDeclaration(Namespace.getNamespace(namespace));
        }
        else
        {
            element.setNamespace(Namespace.getNamespace(namespace));
        }
    }
}
