/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas;

import java.util.HashMap;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.pm.Template;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.XmlNode;



public class Organization extends LdapObjectBase 
{
	//private static final Environment env=Environment.get();
	
	//Collection to hold the users
	public final ChildList<User> users= new ChildList<User>(this, "cn=organizational users,", User.class);
	public final ChildList<User> user = users;
	public final ChildList<User> u    = users;

	//Collection to hold the roles
	public final ChildList<Role> roles= new ChildList<Role>(this, "cn=organizational roles,", Role.class);
	public final ChildList<Role> role= roles;
	public final ChildList<Role> r   = roles;

	//Collection to hold the web service interfaces
	public final ChildList<WebServiceInterface> webServiceInterfaces= new ChildList<WebServiceInterface>(this, "cn=method sets,", WebServiceInterface.class);
	public final ChildList<WebServiceInterface> wsi = webServiceInterfaces;

	//Collection to hold the service groups
	public final ChildList<ServiceGroup> serviceGroups= new ChildList<ServiceGroup>(this, "cn=soap nodes,", ServiceGroup.class);
	public final ChildList<ServiceGroup> sg = serviceGroups;
	
	//Collection to hold the dso types
	public final ChildList<DsoType> dsotypes= new ChildList<DsoType>(this, "cn=data sources,", DsoType.class);
	public final ChildList<DsoType> dsotype = dsotypes;
	
	//Collection to hold the service containers
	public final CordysObjectList<ServiceContainer> serviceContainers; 
	public final CordysObjectList<ServiceContainer> sc; 
	
	//Collection to hold the bpms
	public final ProcessModel.List processes=new ProcessModel.List(this); 
	public final ProcessModel.List proc=processes; 

	//Collection to hold the xmlstore objects
	public final XMLStoreObject.List xmlStoreObjects=new XMLStoreObject.List(this);
	public final XMLStoreObject.List xso=xmlStoreObjects;
	public final XMLStoreObject.List x=xmlStoreObjects;
	
	//Collection to hold the dsos
	public final ChildList<Dso> dsos;
	public final ChildList<Dso> dso;
	public final ChildList<Dso> d;
	
	@SuppressWarnings("unchecked")
	protected Organization(LdapObject parent, String dn) 
	{
		super(parent, dn);
		//Populate the service containers collection
		serviceContainers = new CordysObjectList(parent.getSystem()) 
		{
			@Override protected void retrieveList() 
			{
				for(ServiceGroup serviceGroup: serviceGroups) 
				{
					for (ServiceContainer serviceContainer: serviceGroup.serviceContainers)
						grow(serviceContainer);
				}
			}
			@Override public String getKey()
			{ 
				return "ServiceContainers:"+getDn(); 
			}
		}; 
		sc = serviceContainers;
		
		//Populate the dsos collection
		dsos = new ChildList(parent.getSystem(), Dso.class) 
		{
			@Override protected void retrieveList() 
			{
				for (DsoType dsotype: dsotypes) 
				{
					for (Dso dso: dsotype.dsos)
						grow(dso);
				}
			}
			@Override public String getKey() 
			{ 
				return "dsos:"+getDn(); 
			}
		};
		dso=dsos;
		d=dsos;
	}

	@Override protected String prefix() 
	{ 
		return "org"; 
	}

	public String call(String request) 
	{
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("receiver", getDn());
		return getSystem().call(request, queryParams); 
	}

	@Override protected void preDeleteHook() 
	{
		for (ServiceContainer serviceContainer: serviceContainers)
			serviceContainer.stop();
	}

	/**
	 * Creates a web service interface
	 * 
	 * @param name Name of the web service interface
	 * @param namespace Namespace of the web service
	 * @param implementationclass
	 */
	public void createWebServiceInterface(String name, String namespace, String implementationclass) 
	{
		XmlNode newEntry=newEntryXml("cn=method sets,", name,"busmethodset");
		newEntry.add("labeleduri").add("string").setText(namespace);
		newEntry.add("implementationclass").add("string").setText(implementationclass);
		createInLdap(newEntry);
		webServiceInterfaces.clear();
	}
	
	
	/**
	 * Creates an organization user with the given name. Creates the authenticated user if doesn't exist
	 * 
	 * @param name Name of the user
	 */
	public void createUser(String name)
	{
		AuthenticatedUser authUser=getSystem().authenticatedUsers.getByName(name);
		createUser(name,authUser);
	}
	
	/**
	 * Creates an organization user with the given name
	 * 
	 * @param name Name of the user
	 * @param authUser Authenticated user object
	 */
	public void createUser(String name, AuthenticatedUser authUser) 
	{
		if(authUser==null)
		{
			Environment.get().info("Cound not find authenticated user for '"+name+"'. Hence creating it");
			getSystem().createAuthenticatedUser(name, this.getDn()); 
			authUser=getSystem().authenticatedUsers.getByName(name);
		}
		
		//Create organizational user with the given user name
		XmlNode newEntry=newEntryXml("cn=organizational users,", name,"busorganizationaluser","busorganizationalobject");
		newEntry.add("authenticationuser").add("string").setText(authUser.getDn());
		newEntry.add("menu");
		newEntry.add("toolbar");
		newEntry.add("role").add("string").setText("cn=everyoneIn"+getName()+",cn=organizational roles,"+getDn());
		createInLdap(newEntry);
		users.clear();
	}
	
	/**
	 * Creates a role with the given name
	 * 
	 * @param name Role name
	 */
	public void createRole(String name, String type) 
	{
		XmlNode newEntry=newEntryXml("cn=organizational roles,", name,"busorganizationalrole","busorganizationalobject");
		newEntry.add("description").add("string").setText(name);
		newEntry.add("menu");
		newEntry.add("toolbar");
		newEntry.add("busorganizationalroletype").add("string").setText(type);
		newEntry.add("role").add("string").setText("cn=everyoneIn"+getName()+",cn=organizational roles,"+getDn());
		createInLdap(newEntry);
		roles.clear();
	}
	
	/**
	 * Creates dso type
	 * 
	 * @param name Type of dso
	 */
	public void createDsoType(String name)
	{
		XmlNode newEntry=newEntryXml("cn=data sources,", name,"datasourcetype");
		newEntry.add("implementationclass").add("string").setText("com.cordys.dsoconfig.types.CordysDBSO");
		createInLdap(newEntry);
		dsotypes.clear();
	}
	
	/**
	 * Creates a service group with the given name, configuration and web service interfaces
	 * 
	 * @param name Service group name
	 * @param config XmlNode representing service group configuration
	 * @param webServiceInterfaceSet List of web service interfaces to be configured for the service group
	 */
	public void createServiceGroup(String name, XmlNode config, WebServiceInterface ... webServiceInterfaceSet ) 
	{
		XmlNode newEntry=newEntryXml("cn=soap nodes,", name,"bussoapnode");
		newEntry.add("description").add("string").setText(name);
		XmlNode bms = newEntry.add("busmethodsets");
		XmlNode luri= newEntry.add("labeleduri");
		for (WebServiceInterface webServiceInterface: webServiceInterfaceSet) 
		{
			bms.add("string").setText(webServiceInterface.getDn());
			for (String namespace:webServiceInterface.namespaces.get())
				luri.add("string").setText(namespace);
		}
		//remove soapnode_keystore node		
		XmlNode keystore = config.getChild("soapnode_keystore");	
		if (keystore!=null)
		{
			config.remove(keystore);
		}
		newEntry.add("bussoapnodeconfiguration").add("string").setText(config.compact());
		createInLdap(newEntry);
		serviceGroups.clear();
	}
	
	/**
	 * Creates service group with given name and web service interfaces
	 * 
	 * @param name Service group name
	 * @param webServiceInterfaceSet List of web service interfaces to be configured for the service group
	 */
	public void createServiceGroup(String name, WebServiceInterface ... webServiceInterfaceSet ) 
	{		
		XmlNode routing=new XmlNode("routing");
		routing.setAttribute("ui_algorithm", "failover");
		routing.setAttribute("ui_type", "loadbalancing");
		routing.add("numprocessors").setText("100000");
		routing.add("algorithm").setText("algorithm");
		createServiceGroup(name, routing, webServiceInterfaceSet);
	}	
	
	public XmlNode getXml(String key, String version) 
	{ 
		return getSystem().getXml(key, version, getDn()); 
	}
	
	public XmlNode getXml(String key) 
	{ 
		return getSystem().getXml(key, "organization", getDn()); 
	}
	
	public XmlNode deduct(Isvp isvp) 
	{ 
		return deduct(isvp, isvp.getName());	
	}
	
	public XmlNode deduct(String isvpName) 
	{ 
		return deduct(this, isvpName);	
	}

	
	private XmlNode deduct(LdapObject parent, String isvpName)
	{
		XmlNode result=new XmlNode("caaspm");
		result.setAttribute("isvp", isvpName);
		result.setAttribute("org", this.getName());
		for (ServiceGroup serviceGroup : this.serviceGroups)
		{
			XmlNode node=null;
			for (WebServiceInterface webServiceInterface: serviceGroup.webServiceInterfaces) 
			{
				if (webServiceInterface.getParent()==parent) 
				{
					if (node==null) 
					{
						node=result.add("servicegroup");
						node.setAttribute("name", serviceGroup.getName());
					}
					XmlNode child=node.add("wsi");
					child.setAttribute("name", webServiceInterface.getName());
					child.setAttribute("isvp", isvpName);
				}
			}
		}
		for (User user : this.users) 
		{
			XmlNode node=null;
			for (Role role: user.roles) 
			{
				if (role.getParent()==parent) 
				{
					if (node==null) 
					{
						node=result.add("user");
						node.setAttribute("name", user.getName());
					}
					XmlNode child=node.add("role");
					child.setAttribute("name", role.getName());
					child.setAttribute("isvp", isvpName);
				}
			}
		}
		for (Role role : this.roles) 
		{
			XmlNode node=null;
			for (Role subRole: role.roles) 
			{
				if (subRole.getParent()==parent) 
				{
					if (node==null) 
					{
						node=result.add("role");
						node.setAttribute("name", role.getName());
					}
					XmlNode child=node.add("role");
					child.setAttribute("name", subRole.getName());
					child.setAttribute("isvp", isvpName);
				}
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public CordysObjectList<LdapObject> seek(final Role target) 
	{
		return new CordysObjectList(getSystem()) 
		{
			@Override protected void retrieveList() 
			{
				for(User user : users) 
				{
					for(Role role: user.roles) 
					{
						if (role==target)
							grow(user);
					}
				}
				for(Role role : roles) 
				{
					for (Role subRole: role.roles) 
					{
						if (subRole==target) 
							grow(role);
					}
				}
			}
			@Override public String getKey() 
			{ 
				return Organization.this.getKey()+":seek("+target+")";
			}
		};
	}

	@SuppressWarnings("unchecked")
	public CordysObjectList<ServiceGroup> seek(final WebServiceInterface target) 
	{
		return new CordysObjectList(getSystem()) 
		{
			@Override protected void retrieveList() 
			{
				for(ServiceGroup serviceGroup : serviceGroups) 
				{
					for(WebServiceInterface webServiceInterface: serviceGroup.webServiceInterfaces) 
					{
						if (webServiceInterface==target)
							grow(serviceGroup);
					}
				}
			}
			@Override public String getKey() 
			{ 
				return Organization.this.getKey()+":seek("+target+")";
			}
		};
	}
	
	public Template createTemplate(String isvpName) 
	{ 
		return new Template(this, isvpName); 
	} 
}
