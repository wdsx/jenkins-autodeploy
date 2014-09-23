package co.wds.jenkins.autodeploy.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarker {
	private static final String S3_BUCKET = "autodeploy";
	private static final String S3_KEY = "deploy.csv";
	private static final String LINE_FORMAT = "%s,%s,%s,%s,%s";
	
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

	public void updateAutoDeployData(String projectName, String s3Location, String artifactName, String version, String appType) throws IOException {
		TransferManager tm = createTransferManager();
		S3ObjectInputStream inputStream = tm.getAmazonS3Client().getObject(getAwsGetRequest()).getObjectContent();
		
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer);
		String theString = writer.toString();
		
		String content = generateNewData(projectName, s3Location, artifactName, version, appType, theString);

		File file = File.createTempFile(S3_KEY, ".csv.tmp");
		if (!file.exists()) {
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(content);
		bw.close();
		
		tm.getAmazonS3Client().putObject(S3_BUCKET, S3_KEY, file);
	}

	private String generateNewData(String projectName, String s3Location, String artifactName, String version, String appType, String originalData) {
		return originalData + "\n" + String.format(LINE_FORMAT, projectName, s3Location, artifactName, version, appType);
	}
}
