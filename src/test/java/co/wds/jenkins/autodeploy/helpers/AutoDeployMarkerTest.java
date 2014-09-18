package co.wds.jenkins.autodeploy.helpers;

import static co.wds.testingtools.annotations.RandomAnnotation.randomiseFields;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import co.wds.testingtools.annotations.RandomAnnotation.Randomise;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarkerTest {
	private AutoDeployMarker unit;
	private static final String LINE_FORMAT = "%s,%s,%s,%s,%s";
	
	@Randomise public String secretKey;
	@Randomise public String accessKey;
	
	@Randomise public String projectName;
	@Randomise public String s3Location;
	@Randomise public String artifactName;
	@Randomise public String version;
	@Randomise public String appType;
	public String expectedLine;
	
	@Mock TransferManager mockTransferManager;
	@Mock AmazonS3 mockS3Client;
	@Mock S3Object mockS3Object;

	
	@Before
	public void setup() throws Exception {
		randomiseFields(this);
		initMocks(this);
		
		unit = spy(new AutoDeployMarker(accessKey, secretKey));
		when(unit.createTransferManager()).thenReturn(mockTransferManager);
		when(mockTransferManager.getAmazonS3Client()).thenReturn(mockS3Client);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		
		expectedLine = String.format(LINE_FORMAT, projectName, s3Location, artifactName, version, appType);
	}
	
	@Test
	public void shouldGetAwsCredentialsContainingSecretAndAccessKey() throws Exception {
		AWSCredentials credentials = unit.getAwsCredentials();
		
		assertThat(credentials, is(not(nullValue())));
		assertThat(credentials.getAWSAccessKeyId(), is(accessKey));
		assertThat(credentials.getAWSSecretKey(), is(secretKey));
	}
	
	@Test
	public void shouldCreateAGetTransferRequest() throws Exception {
		GetObjectRequest request = unit.getAwsGetRequest();
		
		assertThat(request, is(not(nullValue())));
		assertThat(request.getBucketName(), is("autodeploy"));
		assertThat(request.getKey(), is("deploy.csv"));
	}
	
	@Test
	public void shouldDownloadAndUpdateEmptyExistingFile() throws Exception {
		InputStream inputStream = IOUtils.toInputStream("");
		S3ObjectInputStream s3InputStream = new S3ObjectInputStream(inputStream, new HttpGet());
		when(mockS3Object.getObjectContent()).thenReturn(s3InputStream);
		
		unit.updateAutoDeployData(projectName, s3Location, artifactName, version, appType);
		
		InOrder order = inOrder(unit, mockTransferManager, mockS3Client, mockS3Object);
		
		ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);

		order.verify(unit).createTransferManager();
		order.verify(mockTransferManager).getAmazonS3Client();
		order.verify(unit).getAwsGetRequest();
		order.verify(mockS3Client).getObject(unit.getAwsGetRequest());
		order.verify(mockS3Object).getObjectContent();
		order.verify(mockTransferManager).getAmazonS3Client();
		order.verify(mockS3Client).putObject(eq("autodeploy"), eq("deploy.csv"), captor.capture());
		
		File file = captor.getValue();
		assertThat(file, is(not(nullValue())));
		String contents = FileUtils.readFileToString(file);
		
		assertThat(contents, is(not(nullValue())));
		assertThat(contents, containsString(expectedLine));
		assertThat(contents.trim(), is(expectedLine));
	}
}
