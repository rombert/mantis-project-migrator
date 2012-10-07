package ro.lmn.mantis.mpm.internal;

import static com.google.common.base.Objects.equal;
import static java.util.Arrays.asList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import biz.futureware.mantis.rpc.soap.client.MantisConnectPortType;
import biz.futureware.mantis.rpc.soap.client.ProjectData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

public class HandleImpl implements Handle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HandleImpl.class);

	private final MantisConnectPortType mantisConnectPort;
	private final String username;
	private final String password;
	private final BigInteger projectId;

	public HandleImpl(MantisConnectPortType mantisConnectPort, String username, String password, int targetProjectId) throws Exception {
		
		this.mantisConnectPort = mantisConnectPort;
		this.username = username;
		this.password = password;
		this.projectId = BigInteger.valueOf(targetProjectId);
		
		for ( ProjectData project :  mantisConnectPort.mc_projects_get_user_accessible(username, password) )
			if ( project.getId().equals(projectId) )
				return;
		
		throw new IllegalArgumentException("User " + username + " does not have access to project with id " + targetProjectId + " on " + mantisConnectPort);
	}
	
	@Override
	public List<ProjectVersionData> getVersions() throws Exception {
		return Arrays.asList(mantisConnectPort.mc_project_get_versions(username, password, projectId));
	}
	
	@Override
	public void synchronizeVersions(List<ProjectVersionData> newVersions) throws Exception {
		
		LOGGER.info("Synchronizing versions");
		
		Map<String, ProjectVersionData> ourVersionsMap = Maps.newHashMap();
		for( ProjectVersionData ourVersion : getVersions() )
			ourVersionsMap.put(ourVersion.getName(), ourVersion);
		
		for( ProjectVersionData newVersion : newVersions ) {
			
			ProjectVersionData toCreate = new ProjectVersionData(null, newVersion.getName(), projectId, 
					newVersion.getDate_order(), newVersion.getDescription(), newVersion.getReleased(), newVersion.getObsolete());
			newVersion.setProject_id(projectId);
			
			normalizeDescription(newVersion);

			ProjectVersionData ourVersion = ourVersionsMap.get(toCreate.getName());
			if ( ourVersion == null ) {
				LOGGER.info("Creating new version with name {} ", toCreate.getName());
				mantisConnectPort.mc_project_version_add(username, password, newVersion);
			} else {
				normalizeDescription(ourVersion);
				if ( !versionEq(ourVersion, newVersion)) {
					LOGGER.info("Updating existing version with name {}", toCreate.getName());
					mantisConnectPort.mc_project_version_update(username, password, ourVersion.getId(), toCreate);
				} else {
					LOGGER.info("Version with name {} already exists and is identical, skipping.", toCreate.getName());
				}
			}
		}
		
		LOGGER.info("Synchronized versions");
	}
	
	@Override
	public List<String> getCategories() throws Exception {
		return asList(mantisConnectPort.mc_project_get_categories(username, password, projectId));
	}
	
	@Override
	public void synchronizeCategories(List<String> newCategories) throws Exception {

		LOGGER.info("Synchronizing categories");
		
		List<String> ourCategories = getCategories();

		for ( String newCategory : newCategories ) {
			if ( !ourCategories.contains(newCategory) ) {
				LOGGER.info("Creating new category {}.", newCategory);
				mantisConnectPort.mc_project_add_category(username, password, projectId, newCategory);
			} else {
				LOGGER.info("Category with name {} already exists, skipping.", newCategory);
			}
		}
		
		LOGGER.info("Synchronized categories");
	}

	private static void normalizeDescription(ProjectVersionData version) {
		
		if ( version.getDescription() == null )
			version.setDescription("");
	}

	private static boolean versionEq(ProjectVersionData ourVersion, ProjectVersionData newVersion) {

		return equal(ourVersion.getName(), newVersion.getName()) && equal(ourVersion.getDate_order(), newVersion.getDate_order()) &&
				equal(ourVersion.getDescription(), newVersion.getDescription()) && equal(ourVersion.getObsolete(), newVersion.getObsolete()) &&
				equal(ourVersion.getReleased(), newVersion.getReleased());
	}
	
}
