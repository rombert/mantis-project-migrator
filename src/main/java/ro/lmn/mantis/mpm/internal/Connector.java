package ro.lmn.mantis.mpm.internal;

public interface Connector {

	Handle connect(String url, String username, String password, int targetProjectId) throws Exception;
}
