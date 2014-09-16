package co.wds.jenkins.autodeploy.helpers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarker {
	private AWSCredentials credentials;

	public AutoDeployMarker(String accessKey, String secretKey) {
		credentials = new BasicAWSCredentials(accessKey, secretKey);
	}

	TransferManager createTransferManager(AWSCredentials credentials) {
		return new TransferManager(credentials);
	}

	public AWSCredentials getAwsCredentials() {
		return credentials;
	}
}
