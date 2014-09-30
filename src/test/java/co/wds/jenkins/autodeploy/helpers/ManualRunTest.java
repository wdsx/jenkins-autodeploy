package co.wds.jenkins.autodeploy.helpers;

import static org.junit.Assert.*;

import org.junit.Test;

public class ManualRunTest {
	private static final String SECRET_ACCESS_KEY = "FUf5XzYeKGf84mU3yTMVo0bLwzS4/vDAAXKPc8ij";
	private static final String ACCESS_KEY = "AKIAJS6CXKH44V6A7ZIA";

	@Test
	public void manualRun() throws Exception {
		AutoDeployMarker marker = new AutoDeployMarker(ACCESS_KEY, SECRET_ACCESS_KEY);
		
		try {
			marker.updateAutoDeployData("test-project", "location", "artifactName", "version", "appType");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
