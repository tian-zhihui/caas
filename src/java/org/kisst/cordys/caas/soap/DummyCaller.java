/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.soap;

import static org.kisst.cordys.caas.main.Environment.trace;

import java.util.HashMap;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.XmlNode;

public class DummyCaller implements SoapCaller
{
    private final XmlNode dump;
    private final HashMap<String, XmlNode> index = new HashMap<String, XmlNode>();

    public DummyCaller(XmlNode dump)
    {
        this.dump = dump;
        addFromXmlDump(dump.getChild("ldap"));
    }

    private void addFromXmlDump(XmlNode ldap)
    {
        XmlNode entry = ldap.getChild("entry");
        String dn = entry.getAttribute("dn");
        index.put(dn, ldap);
        for (XmlNode n : ldap.getChild("children").getChildren())
            addFromXmlDump(n);
    }

    public String call(String input)
    {
        throw new RuntimeException("HTTP calls not supported " + input);
    }

    public String call(String input, String org, String processor)
    {
        throw new RuntimeException("HTTP calls not supported " + input);
    }

    public XmlNode call(XmlNode method)
    {
        return call(method, null, null);
    }

    public XmlNode call(XmlNode method, String org, String processor)
    {
        if (Environment.trace)
            trace(method.getPretty());
        XmlNode output;
        if (method.getName().equals("GetLDAPObject"))
            output = getObject(method);
        else if (method.getName().equals("GetChildren"))
            output = getChildren(method);
        else if (method.getName().equals("GetVersion"))
            output = getVersion();
        else
            throw new RuntimeException("Unknown calls " + method.getPretty());

        if (Environment.trace)
            trace(output.getPretty());
        return output;
    }

    private XmlNode getObject(XmlNode method)
    {
        String dn = method.getChildText("dn");
        XmlNode result = new XmlNode("GetLDAPObjectResponse");
        XmlNode ldap = index.get(dn);
        XmlNode entry;
        if (ldap == null)
        {
            entry = new XmlNode("entry");
            entry.setAttribute("dn", dn);
            entry.add("objectclass").add("string").setText("top");
        }
        else
            entry = ldap.getChild("entry").clone();
        result.add("tuple").add("old").add(entry);
        return result;
    }

    private XmlNode getChildren(XmlNode method)
    {
        String dn = method.getChildText("dn");
        XmlNode result = new XmlNode("GetChildrenResponse");
        XmlNode ldap = null;
        String tmp = dn;
        while (ldap == null && tmp.indexOf(",") > 0)
        {
            ldap = index.get(tmp);
            tmp = tmp.substring(tmp.indexOf(",") + 1);
        }
        for (XmlNode child : ldap.getChild("children").getChildren())
            result.add("tuple").add("old").add(child.getChild("entry").clone());
        return result;
    }

    private XmlNode getVersion()
    {
        XmlNode env = new XmlNode("Envelope");
        XmlNode comp = env.add("Header").add("header").add("sender").add("component");
        comp.setText("cn=LDAP Service,cn=soap nodes,o=system," + dump.getChildText("ldap/@dn"));
        XmlNode result = env.add("Body").add("GetVersionResponse");
        result.add("version").setText(dump.getAttribute("version"));
        result.add("build").setText(dump.getAttribute("build"));
        return result;
    }

    public String getName()
    {
        return dump.getAttribute("name");
    }

    @Override
    public String call(String input, HashMap<String, String> map)
    {
        // TODO Auto-generated webService stub
        return null;
    }

    @Override
    public XmlNode call(XmlNode method, HashMap<String, String> map)
    {
        // TODO Auto-generated webService stub
        return null;
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#httpCall(java.lang.String, java.lang.String, java.util.HashMap)
     */
    @Override
    public String httpCall(String url, String input, HashMap<String, String> queryStringMap)
    {
        return null;
    }
    public String httpCall(String input) { return httpCall(input, null); }
    public String httpCall(String input, HashMap<String, String> map) { return null; }


    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#getUrlBase()
     */
    @Override
    public String getUrlBase()
    {
        return "http://server/cordys";
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#isOLDEnabled()
     */
    @Override
    public boolean isOLDEnabled()
    {
        return false;
    }

    @Override
    public String getUsername()
    {
        return "dummy";
    }

    @Override
    public String call(String request, HashMap<String, String> queryParams, long timeout)
    {
        return null;
    }

    @Override
    public XmlNode call(XmlNode request, HashMap<String, String> queryParams, long timeout)
    {
        return null;
    }

    @Override
    public String call(String request, long timeout)
    {
        return null;
    }

    @Override
    public XmlNode call(XmlNode request, long timeout)
    {
        return null;
    }
}
