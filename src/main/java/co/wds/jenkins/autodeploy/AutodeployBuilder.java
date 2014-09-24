package co.wds.jenkins.autodeploy;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import co.wds.jenkins.autodeploy.helpers.AutoDeployMarker;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link AutodeployBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class AutodeployBuilder extends Builder {
	private String projectName;
	private String s3Location;
	private String artifactName;
	private String version;
	private String appType;

    @DataBoundConstructor
    public AutodeployBuilder(String projectName, String s3Location, String artifactName, String version, String appType) {
		this.projectName = projectName;
		this.s3Location = s3Location;
		this.artifactName = artifactName;
		this.version = version;
		this.appType = appType;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	AutoDeployMarker marker = new AutoDeployMarker(getDescriptor().getS3AccessKey(), getDescriptor().getS3SecretKey());

    	try {
			marker.updateAutoDeployData(getProjectName(), getS3Location(), getArtifactName(), getVersion(), getAppType());
		} catch (IOException e) {
			e.printStackTrace(listener.getLogger());
			return false;
		}

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public String getProjectName() {
		return projectName;
	}

	public String getS3Location() {
		return s3Location;
	}

	public String getArtifactName() {
		return artifactName;
	}

	public String getVersion() {
		return version;
	}

	public String getAppType() {
		return appType;
	}

    /**
     * Descriptor for {@link AutodeployBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    	private String s3AccessKey;
    	private String s3SecretKey;

    	public String getS3AccessKey() {
			return s3AccessKey;
		}

		public String getS3SecretKey() {
			return s3SecretKey;
		}

		/**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckProjectName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value == null || value.length() == 0) {
                return FormValidation.error("Please set a project name");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Generate Autodeploy configuration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        	this.s3AccessKey = formData.getString("s3AccessKey");
        	this.s3SecretKey = formData.getString("s3SecretKey");
            save();
            return super.configure(req,formData);
        }
    }
}

