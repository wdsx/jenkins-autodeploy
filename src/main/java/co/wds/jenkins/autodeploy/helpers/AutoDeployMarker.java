package co.wds.jenkins.autodeploy.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarker {
	private static final String S3_BUCKET = "autodeploy";
	private static final String S3_KEY = "deploy_test.csv";
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
		AmazonS3 amazonS3Client = tm.getAmazonS3Client();
		GetObjectRequest awsGetRequest = getAwsGetRequest();
		S3Object s3object = amazonS3Client.getObject(awsGetRequest);
		S3ObjectInputStream inputStream = s3object.getObjectContent();
		
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
		
		amazonS3Client.putObject(S3_BUCKET, S3_KEY, file);
	}

	private String generateNewData(String projectName, String s3Location, String artifactName, String version, String appType, String originalData) {
		String[] lines = originalData.split("\n");
		String formattedLine = String.format(LINE_FORMAT, projectName, s3Location, artifactName, version, appType);
		StringBuffer newLines = new StringBuffer();
		
		boolean addedLine = false;
		for (String line : lines) {
			if (newLines.length() > 0) {
				newLines.append("\n");
			}
			if (line.startsWith(projectName)) {
				newLines.append(formattedLine);
				addedLine = true;
			} else {
				newLines.append(line);
			}
		}
		
		if (!addedLine) {
			newLines.append(formattedLine);
		}
		
		
		return newLines.toString();
	}
}
