/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.cm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This is the main class for the configuration manager. The configuration manager has a few main operations:
 * <ul>
 * <li>check - Checks if the configuration of the objectives that are in the ccm file are currently in place</li>
 * <li>configure - Configures the given organization based on the current ccm file.</li>
 * <li>purge - Removes the configuration as described in the ccm file from the given organization.</li>
 * </ul>
 */
public class CaasPackage implements Objective
{
    /** Holds the objectives that are currently configured. */
    private final LinkedList<Objective> objectives = new LinkedList<Objective>();
    /** Holds the organization that needs to be configured. */
    private final String orgName;
    /** Holds the current status */
    private int status;
    /** Holds the name of the CCM file */
    private final String name;
    /** Holds the system to work with */
    private final CordysSystem system;

    /**
     * Instantiates a new caas package. It will load the given pmFile and parse the objectives from the definition.
     * 
     * @param ccmfile The pmfile to parse.
     */
    public CaasPackage(String ccmfile, CordysSystem system, String organization)
    {
        this.name = ccmfile;
        this.system = system;
        String content = FileUtil.loadString(ccmfile);

        // The content of the file contains the variables. So we need to replace them which the actual properties.
        Map<String, String> variables = Environment.get().loadSystemProperties(system.getName(), organization);
        content = StringUtil.substitute(content, variables);

        XmlNode pm = new XmlNode(content);
        orgName = pm.getAttribute("org");
        Organization org = system.organizations.get(orgName);

        for (XmlNode child : pm.getChildren())
        {
            if ("servicegroup".equals(child.getName()))
            {
                objectives.add(new ServiceGroupObjective(org, child));
            }
            else if ("user".equals(child.getName()))
            {
                objectives.add(new UserObjective(org, child));
            }
            else if ("package".equals(child.getName()))
            {
                objectives.add(new PackageObjective(org, child));
            }
            else if ("dso".equals(child.getName()))
            {
                objectives.add(new DsoObjective(org, child));
            }
            else
            {
                Environment.warn("Unknown objective: " + child.getName());
            }
        }
    }

    /**
     * This method gets the name of the file.
     * 
     * @return The name of the file.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "CaasPackage(" + name + ")";
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getChildren()
     */
    public List<Objective> getChildren()
    {
        return objectives;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getMessage()
     */
    public String getMessage()
    {
        return "see Children";
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getStatus()
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getSystem()
     */
    public CordysSystem getSystem()
    {
        return system;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#check(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    public int check(Ui ui)
    {
        ui.checking(this);
        status = OK;
        for (Objective o : objectives)
            status = Math.max(status, o.check(ui));
        ui.readyWith(this);

        return status;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#configure(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    public void configure(Ui ui)
    {
        ui.configuring(this);
        for (Objective o : objectives)
            o.configure(ui);
        ui.readyWith(this);
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#purge(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    public void purge(Ui ui)
    {
        for (Objective o : objectives)
            o.purge(ui);
    }

    /**
     * This method returns the default organization name that is used.
     * 
     * @return The default organization name that is used.
     */
    public String getDefaultOrgName()
    {
        return orgName;
    }
}
