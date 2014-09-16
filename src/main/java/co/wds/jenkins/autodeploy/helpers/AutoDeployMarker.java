package co.wds.jenkins.autodeploy.helpers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarker {
	private static final String S3_BUCKET = "autodeploy";
	private static final String S3_KEY = "deploy.csv";
	
	private AWSCredentials credentials;
	private GetObjectRequest getObjectRequest;

	public AutoDeployMarker(String accessKey, String secretKey) {
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		getObjectRequest = new GetObjectRequest(S3_BUCKET, S3_KEY);
	}

	TransferManager createTransferManager() {
		return new TransferManager(getAwsCredentials());
	}

	public AWSCredentials getAwsCredentials() {
		return credentials;
	}

	public GetObjectRequest getAwsGetRequest() {
		return getObjectRequest;
	}
}
