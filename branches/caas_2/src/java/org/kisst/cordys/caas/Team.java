package org.kisst.cordys.caas;

import java.util.ArrayList;
import java.util.List;

import org.kisst.cordys.caas.Assignment.AssignmentList;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.XmlProperty;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class wraps the details of a team definition within an organization.
 * 
 * @author pgussow
 */
public class Team extends CordysObject
{
    /** The space in which a team can be defined */
    public enum ESpace
    {
        organization, isv
    };

    /** Holds the organization in which the team resides */
    private Organization m_org;
    /** Holds the XML data for this unit. */
    private XmlNode m_unitData;
    /** Holds the fully qualified name of the unit. */
    public final XmlProperty<String> qname;
    /** Holds the space if which the team is defined. */
    public final XmlProperty<String> space;
    /** Holds the ID of the team. */
    public final XmlProperty<String> id;
    /** Holds the name of the team. */
    public final XmlProperty<String> teamName;
    /** Holds the description of the team. */
    public final XmlProperty<String> description;
    /** Holds whether or not the team is deleted. */
    public final XmlProperty<Boolean> deleted;
    /** Holds the assignments to this team */
    public final AssignmentList<Team> assignments;
    /** Alias */
    public final AssignmentList<Team> assignment;
    /** Alias */
    public final AssignmentList<Team> a;

    /**
     * Instantiates a new team.
     * 
     * @param org The org
     * @param unit The fqn
     */
    public Team(Organization org, XmlNode unit)
    {
        m_org = org;
        m_unitData = unit.clone();

        id = new XmlProperty<String>(m_unitData, "", "ID", "ua", Constants.NS, String.class);
        teamName = new XmlProperty<String>(m_unitData, "", "Name", "ua", Constants.NS, String.class);
        qname = new XmlProperty<String>(m_unitData, "", "QName", "ua", Constants.NS, String.class);
        description = new XmlProperty<String>(m_unitData, "", "Description", "ua", Constants.NS, String.class);
        deleted = new XmlProperty<Boolean>(m_unitData, "", "Deleted", "ua", Constants.NS, Boolean.class);
        space = new XmlProperty<String>(m_unitData, "", "Space", "ua", Constants.NS, String.class);

        // Create the assignment list
        assignments = new AssignmentList<Team>(this);
        assignment = assignments;
        a = assignments;
    }

    @Override
    public CordysSystem getSystem()
    {
        return m_org.getSystem();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return qname.get();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return m_org.getVarName() + ".teams.\"" + qname.get() + "\"";
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return qname.get();
    }

    /**
     * This method gets the organization to which this team is bound.
     * 
     * @return The organization to which this team is bound.
     */
    public Organization getOrganization()
    {
        return m_org;
    }

    /**
     * This method gets the XML data for this unit.
     * 
     * @return The XML data for this unit.
     */
    public XmlNode getUnitData()
    {
        return m_unitData;
    }

    /**
     * This method sets the XML data for this unit.
     * 
     * @param unitData The XML data for this unit.
     */
    public void setUnitData(XmlNode unitData)
    {
        m_unitData = unitData;
    }

    /**
     * Holds the list of all the teams in the organization.
     */
    public static class TeamList extends CordysObjectList<Team>
    {
        /** Holds the organization to get the teams for */
        private Organization m_organization;

        /**
         * Instantiates a new list.
         * 
         * @param organization The organization
         */
        protected TeamList(Organization organization)
        {
            super(organization.getSystem());
            m_organization = organization;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // Get all the teams that are accessible via this organization
            XmlNode request = new XmlNode(Constants.GET_UNITS_FOR_ASSIGNMENT, Constants.XMLNS_USER_ASSIGNMENT);
            request.add("WorkspaceID").setText("__Organization Staging__");
            request.add("AssignmentRoot").setText(m_organization.getAssignmentRoot());
            request.add("Filter").setText(".*.*");
            request.add("UseRegEx").setText("true");

            XmlNode response = m_organization.call(request);

            List<XmlNode> units = response.xpath("//ua:GetUnitsForAssignments/ua:dataset/ua:tuple/ua:old/ua:Unit", Constants.NS);
            for (XmlNode unit : units)
            {
                Team t = new Team(m_organization, unit);
                grow(t);
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return m_organization.getVarName() + ".teams";
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return m_organization;
        }
        
        /**
         * This method will search for the team with the given Unit ID.
         * 
         * @param unitId The ID of the team to be found.
         * @return The team with the given ID.
         */
        public Team findByUnitID(String unitId)
        {
            Team retVal = null;
            
            //Make sure the list is available
            ArrayList<Team> allTeams = fetchList();
            
            for (Team team : allTeams)
            {
                if (team.id.get().equals(unitId))
                {
                    retVal = team;
                    break;
                }
            }
            
            return retVal;
        }
    }
}
