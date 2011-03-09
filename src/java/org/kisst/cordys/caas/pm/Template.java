package org.kisst.cordys.caas.pm;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.Configuration;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.Isvp;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.MethodSet;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.SoapNode;
import org.kisst.cordys.caas.SoapProcessor;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.XMLStoreObject;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

public class Template {
	private static final Environment env=Environment.get();
	private final String template;
	public Template(String template) {this.template=template; }
	
	public Template(Organization org, String targetIsvpName) {
		XmlNode result=new XmlNode("org");

		//result.setAttribute("isvp", isvpName);
		result.setAttribute("org", org.getName());
		for (SoapNode sn : org.soapNodes) {
			XmlNode node=result.add("soapnode");
			node.setAttribute("name", sn.getName());
			node.add("bussoapnodeconfiguration").add(sn.config.getXml().clone());
			for (MethodSet ms: sn.methodSets) {
				XmlNode child=node.add("ms");
				child.setAttribute("name", ms.getName());
				String isvpName=null;
				if (ms.getParent() instanceof Organization)
					isvpName=targetIsvpName;
				else
					isvpName=ms.getParent().getName();
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
			for (SoapProcessor sp: sn.soapProcessors) {
				XmlNode child=node.add("sp");
				child.setAttribute("name", sp.getName());
				child.setAttribute("automatic", ""+sp.automatic.getBool());
				child.add("bussoapprocessorconfiguration").add(sp.config.getXml().clone());
				for (ConnectionPoint cp: sp.connectionPoints) {
					XmlNode cpNode=node.add("cp");
					cpNode.setAttribute("name", cp.getName());
				}
			}
		}
		for (User u : org.users) {
			if ("SYSTEM".equals(u.getName().toUpperCase()))
				continue; // SYSTEM user should not be part of the template
			XmlNode node=result.add("user");
			node.setAttribute("name", u.getName());
			node.setAttribute("au", u.au.getRef().getName());
			for (Role r: u.roles) {
				String isvpName=null;
				if (r.getParent() instanceof Organization) {
					if (r.getName().equals("everyoneIn"+org.getName()))
						continue;
					isvpName=targetIsvpName;
				}
				else
					isvpName=r.getParent().getName();
				XmlNode child=node.add("role");
				child.setAttribute("name", r.getName());
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
		}
		for (Role rr : org.roles) {
			XmlNode node=result.add("role");
			node.setAttribute("name", rr.getName());
			for (Role r: rr.roles) {
				String isvpName=null;
				if (r.getParent() instanceof Organization) {
					if (r.getName().equals("everyoneIn"+org.getName()))
						continue;
					isvpName=targetIsvpName;
				}
				else
					isvpName=r.getParent().getName();
				XmlNode child=node.add("role");
				child.setAttribute("name", r.getName());
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
		}
		String str=result.getPretty();
		this.template=str.replace("$", "${dollar}");
	}//End of Template constructor

	public void save(String filename) { FileUtil.saveString(new File(filename), template);}


	public XmlNode xml( Map<String, String> vars) {
		String str=template;
		if (vars!=null)
			str=StringUtil.substitute(str, vars);
		str=str.replace("${dollar}", "$");
		return new XmlNode(str);
	}
	public void apply(Organization org, Configuration conf) { apply(org, conf.getProps()); }
	public void apply(Organization org, Map<String, String> vars) {
		XmlNode template=xml(vars);
		for (XmlNode node : template.getChildren()){
			if ((node.getName().equals("soapnode"))|| (node.getName().equals("servicegroup")))
				processSoapNode(org, node);
			else if (node.getName().equals("user"))
				processUser(org, node);
			else if (node.getName().equals("role"))
				processRole(org, node);
			//Check if the template has XMLStore content in it
			else if (node.getName().equals("xmlstoreobject"))
				processXMLStoreObject(org, node);
			else
				System.out.println("Unknown organization element "+node.getPretty());
		}
	}

	/**
	 * Processes the XMLStore operations 
	 * 
	 * @param org
	 * @param node
	 */
	private void processXMLStoreObject(Organization org, XmlNode node) {
			boolean updateFlag = Boolean.valueOf(node.getAttribute("update"));
			String key = node.getAttribute("key");
			String version = node.getAttribute("version");
			XmlNode newXml = node.getChildren().get(0);
			XMLStoreObject obj = new XMLStoreObject(key,version,org);
			obj.updateXML(newXml.clone(), updateFlag);
	}
	
	/**
	 * 
	 * If the update attribute is set to true on the element, 
	 * then it will update the respective configuration.
	 * Update the methodsets and namespace all at once.  
	 * 
	 * @param org
	 * @param node
	 */
	private void processSoapNode(Organization org, XmlNode node) {
		
		//Read the SG name from the configuration
		String name=node.getAttribute("name");
		
		//Check if the SG is already existing by comparing its name
		SoapNode sn=org.soapNodes.getByName(name);
		
		//Create a new SG if it's not existing
		if (sn==null) {
			env.info("creating soapnode "+name);
			XmlNode config=node.getChild("bussoapnodeconfiguration").getChildren().get(0).clone();
			org.createSoapNode(name, config, getMs(org,node));
			sn=org.soapNodes.getByName(name);
		}
		//Update the existing SG with new configuration
		else {
			//Check the 'update' flag in the configuration 
			String updateFlag = node.getAttribute("update");
			if ((updateFlag==null) || (updateFlag.equalsIgnoreCase("false")))
			{
				env.error("Skipping updating service group '"+name+"'.Set 'update' attribute to true to overwrite");
				return;
			}
			
			//Update the method sets of the SG
			env.info("updating methodsets of soapnode "+name);
			MethodSet[] newMethodSets = getMs(org,node);
			if ((newMethodSets!=null)&&(newMethodSets.length > 0))
			{
				sn.ms.update(newMethodSets);
				ArrayList<String> namepsaces = new ArrayList<String>();
				for (MethodSet methodSet : newMethodSets) {				
					for (String ns : methodSet.namespaces.get()) {
						namepsaces.add(ns);
					}
				}			
				sn.namespaces.update(namepsaces);	
			}
		}
		//Proceed with the SC creation or updation
		for (XmlNode child:node.getChildren()) 
		{
			//TODO: Need to add another condition to check for the 'wsi' string to keep it align with BOP-4 terminology
			if ((child.getName().equals("ms")))
				continue;
			else if ((child.getName().equals("sp"))||(child.getName().equals("sc"))) 
			{				
				//Get the machine objects
				CordysObjectList<Machine> machines = org.getSystem().machines;	
				//Check if it is Cordys clustered installation
				boolean isClustered = machines.getSize()>1;
				//Iterate over the machines objects
				for(int i=0;i<machines.getSize();i++)
				{
					//Read the SC name
					String spname=child.getAttribute("name");
					//Read the machine name
					String machineName = machines.get(i).getName();
					//Read the Cordys installation directory path
					String cordysInstallDir = machines.get(i).getCordysInstallDir(); 
					/*
					 * NOTE: 
					 * It is assumed that the following naming convention is followed for SC
					 * Stand alone - SC Name - RMG BPM SC 
					 * Clustered - SC Name_machineName - RMG BPM SC_dev-int-cordys (and) RMG BPM SC_dev-int-aux
					 */
					//Construct the SC name as per the above naming convention, in case of clustered installation
					if(isClustered)
						spname = spname.concat("_").concat(machineName);
					
					//Check if the SC is already existing by comparing its name  
					SoapProcessor sp = sn.soapProcessors.getByName(spname);
					//Update the existing SC with new configuration
					if (sp!=null) 
					{
						env.info("updating existing soap processor "+spname);
						boolean automatic="true".equals(child.getAttribute("automatic"));
						XmlNode config=child.getChild("bussoapprocessorconfiguration").getChildren().get(0);
						
						//Replace the CORDYS_INSTALL_DIR with the corresponding value
						replaceCordysInstallDir(config, cordysInstallDir);
						
						sn.updateSoapProcessor(spname, machineName, automatic, config.clone(),sp);
						spname=null;
						continue;
					}
					//Create a new SC
					else				 
					{
						env.info("creating soap processor "+spname);
						boolean automatic="true".equals(child.getAttribute("automatic"));
						XmlNode config=child.getChild("bussoapprocessorconfiguration").getChildren().get(0);

						//Replace the CORDYS_INSTALL_DIR with the corresponding value
						replaceCordysInstallDir(config, cordysInstallDir);
						
						sn.createSoapProcessor(spname, machineName, automatic, config.clone());
						for (XmlNode subchild:child.getChildren()) {
							if (subchild.getName().equals("cp")) {
								SoapProcessor newSP=sn.sp.getByName(spname);
								newSP.createConnectionPoint(subchild.getAttribute("name"),machineName);
							}	
						}
						spname=null;
					}
				}//end of for loop
			}
			else if (child.getName().equals("bussoapnodeconfiguration")) {}
			else
				System.out.println("Unknown soapnode subelement "+child.getPretty());
		}
	}

	private MethodSet[] getMs(Organization org, XmlNode node) {
		ArrayList<MethodSet> result=new ArrayList<MethodSet>();
		for (XmlNode child:node.getChildren()) {
			//TODO: Need to add another condition to check for the 'wsi' string to keep it align with BOP-4 terminology
			if ((child.getName().equals("ms")) ) {
				MethodSet newms=null;
				String isvpName=child.getAttribute("isvp");
				String msName=child.getAttribute("name");
				if (isvpName==null) {
					newms=org.methodSets.getByName(msName);
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						newms=isvp.methodSets.getByName(msName);
				}
				if (newms!=null) {
					result.add(newms);
				}
				else {
					env.error("Skipping unknownn methodset "+msName);
				}
			}
		}
		return result.toArray(new MethodSet[result.size()]);
	}
	private void processUser(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		if ("SYSTEM".equals(name.toUpperCase())) {
			// Whenever I had a SYSTEM user in my template, Cordys would crash pretty hard.
			// It would not be possible to start the monitor anymore.
			// I had to use the CMC to remove the organization before the Monitor would start again.
			env.error("Ignoring user "+name+" because the SYSTEM user should not be modified from a template");
			return;
		}
		if (org.users.getByName(name)==null) {
			AuthenticatedUser au=org.getSystem().authenticatedUsers.getByName(node.getAttribute("au"));
			if (au==null) {
				env.error("could not create user "+name+" could not find authenticated user "+node.getAttribute("au"));
				//continue;
			}
			else {
				env.info("creating user "+name+" with authenticated user "+au.getName());
				org.createUser(name, au);
			}
		}
		else
			env.info("configuring user "+name);
		User u=org.users.getByName(name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  adding role "+roleName);
				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r!=null)
					u.roles.add(r);
				else
					u.roles.add(dnRole);
			}
			else
				System.out.println("Unknown user subelement "+child.getPretty());
		}
	}

	private void processRole(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		if (org.roles.getByName(name)==null) {
			env.info("creating role "+name);
			org.createRole(name);
		}
		else
			env.info("configuring role "+name);
		Role rr=org.roles.getByName(name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  adding role "+roleName);

				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r!=null)
					rr.roles.add(r);
				else
					rr.roles.add(dnRole);
			}
			else
				System.out.println("Unknown role subelement "+child.getPretty());
		}
	}
	/*
	private Role[] getRoles(Organization org, XmlNode node) {
		ArrayList<Role> result=new ArrayList<Role>();
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r!=null)
					result.add(r);
				else
					env.error("  skipping unknown role "+roleName);
			}
		}
		return result.toArray(new Role[result.size()]);
	}
	*/
	
	public void check(Organization org, Configuration conf) { check(org, conf.getProps()); }
	public void check(Organization org, Map<String, String> vars) {
		XmlNode template=xml(vars);
		for (XmlNode node : template.getChildren()){
			if (node.getName().equals("soapnode"))
				checkSoapNode(org, node);
			else if (node.getName().equals("user"))
				checkUser(org, node);
			else if (node.getName().equals("role"))
				checkRole(org, node);
			else
				System.out.println("Unknown organization element "+node.getPretty());
		}
	}

	private void checkSoapNode(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		SoapNode sn=org.soapNodes.getByName(name);
		if (sn==null) {
			env.error("missing soapnode "+name);
			return;
		}
		env.info("checking configuration of soapnode "+name);
		MethodSet[] target = getMs(org,node);
		for (MethodSet ms : target){
			if (! sn.methodSets.contains(ms))
				env.error("SoapNode "+sn+" does not contain method set "+ms);
		}
		for (MethodSet ms : sn.methodSets){
			boolean found=false;
			for (MethodSet ms2: target) {
				if (ms.getDn().equals(ms2.getDn())) 
					found=true;
			}
			if (! found)
				env.error("SoapNode "+sn+" contains method set "+ms+" that is not in template");
		}
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("ms"))
				continue;
			else if (child.getName().equals("sp")) {
				String spname=child.getAttribute("name");
				SoapProcessor sp = sn.soapProcessors.getByName(spname);
				if (sp==null) {
					env.error("  missing soap processor "+spname);
					continue;
				}
				boolean automatic="true".equals(child.getAttribute("automatic"));
				if (sp.automatic.getBool() != automatic)
					env.error("  "+sp+" property automatic, template says "+automatic+" while current value is "+sp.automatic.get());
				XmlNode config=child.getChild("bussoapprocessorconfiguration").getChildren().get(0);
				XmlNode configsp=sp.config.getXml();
				for (String msg: config.diff(configsp)) 
					env.error(msg);
			}
			else if (child.getName().equals("bussoapnodeconfiguration")) {}
			else
				env.error("Unknown soapnode subelement "+child.getPretty());
		}
	}

	private void checkUser(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		User u=org.users.getByName(name);
		if (u==null) {
			env.info("unknown user "+name);
			return;
		}
		env.info("checking roles of user "+name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  checking role "+roleName);
				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r==null)
					env.error("User "+u+" should have unknown role "+dnRole);
				else if (! u.roles.contains(r))
					env.error("User "+u+" does not have role "+r);
			}
			else
				env.error("Unknown user subelement "+child.getPretty());
		}
	}

	private void checkRole(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		Role rr=org.roles.getByName(name);
		if (rr==null) {
			env.info("Unknowm role "+name);
			return;
		}
		env.info("checking roles of role "+name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  adding role "+roleName);

				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r==null)
					env.error("Role "+rr+" should have unknown role "+dnRole);
				else if (! rr.roles.contains(r))
					env.error("Role "+rr+" does not have role "+r);
			}
			else
				env.error("Unknown role subelement "+child.getPretty());
		}
	}
	
	//NOTE: Please suggest a better way of doing it
	private void replaceCordysInstallDir(XmlNode configNode, String cordysInstallDir){
		
		XmlNode jreConfigNode = configNode.getChild("jreconfig");
		configNode.remove(jreConfigNode);
		String str = jreConfigNode.toString();
		str = str.replaceAll("CORDYS_INSTALL_DIR", cordysInstallDir);
		jreConfigNode = new XmlNode(str);
		configNode.add(jreConfigNode.clone());
	}

}
