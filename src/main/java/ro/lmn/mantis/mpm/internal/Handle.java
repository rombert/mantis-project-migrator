package ro.lmn.mantis.mpm.internal;

import java.util.List;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

public interface Handle {
	
	List<ProjectVersionData> getVersions() throws Exception;
	
	void synchronizeVersions(List<ProjectVersionData> newVersions) throws Exception;
	
	List<String> getCategories() throws Exception;
	
	void synchronizeCategories(List<String> newCategories) throws Exception;
	
	List<IssueData> getIssues(int filterId) throws Exception;
	
	void synchronizeIssues(int filterId, List<IssueData> newIssues, String oldIssueTrackerUrl) throws Exception;
	
	List<AccountData> getUsers() throws Exception;
}
