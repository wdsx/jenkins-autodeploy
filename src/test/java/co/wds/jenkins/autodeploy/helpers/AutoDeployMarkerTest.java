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
import java.io.IOException;
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
	private static final String LINE_5 = "knowledge-frame,xi/knowledge-frame,knowledge-frame-v42.tgz,v42,play2";
	private static final String LINE_4 = "orchestrator,xi/xi-orchestrator,xi-orchestrator-v371.tgz,v371,play2";
	private static final String LINE_3 = "switchboard,xi/xi-switchboard,xi-switchboard-v51.tgz,v51,play2";
	private static final String LINE_2 = "mind-meld,mind-meld,mind-meld-v100.tgz,v100,play2";
	private static final String LINE_1 = "virtual-agent,virtual-agent,virtual-agent-v102.tgz,v102,play2";
	
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
		assertThat(request.getKey(), is("deploy_test.csv"));
	}
	
	@Test
	public void shouldDownloadAndUpdateEmptyExistingFile() throws Exception {
		String contents = checkChangeOriginalDocumentContents("");
		assertThat(contents.trim(), is(expectedLine));
	}
	
	@Test
	public void shouldDownloadAndUpdateExistingFileWithNewData() throws Exception {
		String originalContents = LINE_1 + "\n" + LINE_2 + "\n" + LINE_3 + "\n" + LINE_4 + "\n" + LINE_5;
		String newContents = checkChangeOriginalDocumentContents(originalContents);

		assertThat(newContents, containsString(LINE_1));
		assertThat(newContents, containsString(LINE_2));
		assertThat(newContents, containsString(LINE_3));
		assertThat(newContents, containsString(LINE_4));
		assertThat(newContents, containsString(LINE_5));
	}
	
	@Test
	public void shouldDownloadAndUpdateExistingFileWithNewDataForProjectName() throws Exception {
		String newLine = String.format(LINE_FORMAT, projectName, "OLD_S3", "OLD_ARTIFACT", "OLD_VERSION", "OLD_APP_TYPE");
		String originalContents = LINE_1 + "\n" + newLine;
		String newContents = checkChangeOriginalDocumentContents(originalContents);
		
		assertThat(newContents, containsString(LINE_1));
		assertThat(newContents, not(containsString(newLine)));
	}
	
	@Test
	public void shouldHaveNewLinesAfterEachLineOfData() throws Exception {
		String newLine = String.format(LINE_FORMAT, projectName, "OLD_S3", "OLD_ARTIFACT", "OLD_VERSION", "OLD_APP_TYPE");
		String originalContents = LINE_1 + "\n" + newLine + "\n" + LINE_2 + "\n" + LINE_3;
		String newContents = checkChangeOriginalDocumentContents(originalContents);
		
		String[] newLines = newContents.split("\n");
		assertThat(newLines.length, is(4));
	}

	private String checkChangeOriginalDocumentContents(String originalDocumentContents) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(originalDocumentContents);
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
		order.verify(mockS3Client).putObject(eq("autodeploy"), eq("deploy_test.csv"), captor.capture());
		
		File file = captor.getValue();
		assertThat(file, is(not(nullValue())));
		String contents = FileUtils.readFileToString(file);
		
		assertThat(contents, is(not(nullValue())));
		assertThat(contents, containsString(expectedLine));
		return contents;
	}
}
