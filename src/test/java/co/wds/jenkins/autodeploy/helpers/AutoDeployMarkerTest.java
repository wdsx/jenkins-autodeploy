package co.wds.jenkins.autodeploy.helpers;

import static co.wds.testingtools.annotations.RandomAnnotation.randomiseFields;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import co.wds.testingtools.annotations.RandomAnnotation.Randomise;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;

public class AutoDeployMarkerTest {
	private AutoDeployMarker unit;
	
	@Randomise public String secretKey;
	@Randomise public String accessKey;
	@Mock TransferManager tm;
	
	@Before
	public void setup() throws Exception {
		randomiseFields(this);
		initMocks(this);
		
		unit = spy(new AutoDeployMarker(accessKey, secretKey));
		when(unit.createTransferManager(any(AWSCredentials.class))).thenReturn(tm);
	}
	
	@Test
	public void shouldGetAwsCredentialsContainingSecretAndAccessKey() throws Exception {
		AWSCredentials credentials = unit.getAwsCredentials();
		
		assertThat(credentials, is(not(nullValue())));
		assertThat(credentials.getAWSAccessKeyId(), is(accessKey));
		assertThat(credentials.getAWSSecretKey(), is(secretKey));
	}
}
