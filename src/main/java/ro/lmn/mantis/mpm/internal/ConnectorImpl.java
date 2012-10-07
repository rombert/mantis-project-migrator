package ro.lmn.mantis.mpm.internal;

import java.net.URL;

import biz.futureware.mantis.rpc.soap.client.MantisConnectLocator;
import biz.futureware.mantis.rpc.soap.client.MantisConnectPortType;

public class ConnectorImpl implements Connector {

	@Override
	public Handle connect(String url, String username, String password, int targetProjectId) throws Exception  {
		
		MantisConnectLocator locator = new MantisConnectLocator();

		MantisConnectPortType mantisConnectPort = locator.getMantisConnectPort(new URL(url+"/api/soap/mantisconnect.php"));
		
		return new HandleImpl(mantisConnectPort, username, password, targetProjectId);
	}

}
