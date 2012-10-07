package ro.lmn.mantis.mpm.internal;

import java.util.List;

import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

public interface Handle {
	
	List<ProjectVersionData> getVersions() throws Exception;
	
	void synchronizeVersions(List<ProjectVersionData> newVersions) throws Exception;
	
	List<String> getCategories() throws Exception;
	
	void synchronizeCategories(List<String> newCategories) throws Exception;
}
