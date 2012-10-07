package ro.lmn.mantis.mpm.internal;

import static com.google.common.base.Objects.equal;
import static java.util.Arrays.asList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.MantisConnectPortType;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
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
			else for ( ProjectData subProject : project.getSubprojects() )
				if ( subProject.getId().equals(projectId) )
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
	
	@Override
	public List<IssueData> getIssues(int filterId) throws Exception {
		
		BigInteger perPage = BigInteger.valueOf(50);
		BigInteger currentPage= BigInteger.ONE;
		List<IssueData> allIssues = new ArrayList<>();
		
		while ( true ) {
			IssueData[] issues = mantisConnectPort.mc_filter_get_issues(username, password, projectId, BigInteger.valueOf(filterId), currentPage, perPage);
			
			allIssues.addAll(Arrays.asList(issues));
			
			currentPage = currentPage.add(BigInteger.ONE);
			
			if ( issues.length != perPage.intValue() )
				break;
		}
		
		return allIssues;
	}
	
	@Override
	public void synchronizeIssues(int filterId, List<IssueData> newIssues, String oldIssueTrackerUrl) throws Exception {

		Map<String, IssueData> ourIssues = new HashMap<>();
		for( IssueData ourIssue : getIssues(filterId))
			ourIssues.put(ourIssue.getSummary(), ourIssue); // TODO use a compound issue key

		for ( IssueData newIssue : newIssues ) {
			if ( ourIssues.containsKey(newIssue.getSummary())) {
				LOGGER.info("For issue to import with id {} found issue with id {} and same name {}. Skipping", newIssue.getId(), ourIssues.get(newIssue.getSummary()), newIssue.getSummary());
			} else {
				
				IssueData toCreate = new IssueData();
				toCreate.setAdditional_information(newIssue.getAdditional_information());
				toCreate.setBuild(newIssue.getBuild());
				toCreate.setCategory(newIssue.getCategory());
				toCreate.setDate_submitted(newIssue.getDate_submitted());
				toCreate.setDescription(newIssue.getDescription());
				toCreate.setDue_date(newIssue.getDue_date());
				toCreate.setEta(newIssue.getEta());
				toCreate.setFixed_in_version(newIssue.getFixed_in_version());
				toCreate.setLast_updated(newIssue.getLast_updated());
				toCreate.setOs(newIssue.getOs());
				toCreate.setOs_build(newIssue.getOs_build());
				toCreate.setPlatform(newIssue.getPlatform());
				toCreate.setPriority(newIssue.getPriority());
				toCreate.setProject(new ObjectRef(projectId, null));
				toCreate.setProjection(newIssue.getProjection());
				toCreate.setReproducibility(newIssue.getReproducibility());
				toCreate.setResolution(newIssue.getResolution());
				toCreate.setSeverity(newIssue.getSeverity());
				toCreate.setStatus(newIssue.getStatus());
				toCreate.setSteps_to_reproduce(newIssue.getSteps_to_reproduce());
				toCreate.setSticky(newIssue.getSticky());
				toCreate.setSummary(newIssue.getSummary());
				toCreate.setTarget_version(newIssue.getTarget_version());
				toCreate.setVersion(newIssue.getVersion());
				toCreate.setView_state(newIssue.getView_state());
				
				// TODO - improve handler matching
				AccountData thisUser = new AccountData(null, username, null, null);
				if ( toCreate.getHandler() != null && toCreate.getHandler().getName().equals(username) )
					toCreate.setHandler(thisUser);
				
				List<IssueNoteData> notes = new ArrayList<>();
				
				if ( newIssue.getNotes() != null ) {
					for ( IssueNoteData newNote : newIssue.getNotes() ) {
						// TODO - consider skipping if usernames match
						String text = "Original note author: " + newNote.getReporter().getName()+ "\n\n" + newNote.getText();
						
						notes.add(new IssueNoteData(null, thisUser, text, newNote.getView_state(), 
								newNote.getDate_submitted(), newNote.getLast_modified(), newNote.getTime_tracking(), 
								newNote.getNote_type(), newNote.getNote_attr()));
					}
				}
				
				IssueNoteData importNote = new IssueNoteData();
				importNote.setReporter(thisUser);
				importNote.setText("Originally reported at " + oldIssueTrackerUrl +"/view.php?id="+ newIssue.getId() +" by " + newIssue.getReporter().getName());
				notes.add(importNote);
				
				toCreate.setNotes(notes.toArray(new IssueNoteData[notes.size()]));

				// TODO - tags
				// TODO - relationships
				// TODO - monitors ?
				// TODO - attachments
				
				LOGGER.info("Importing issue {}. [{}] {}", newIssue.getId(), newIssue.getCategory(), newIssue.getSummary());
				
				mantisConnectPort.mc_issue_add(username, password, toCreate);
				
			}
		}
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
