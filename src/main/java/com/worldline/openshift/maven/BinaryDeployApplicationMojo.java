package com.worldline.openshift.maven;

import com.jcraft.jsch.JSchException;
import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Mojo(name = "binary-deploy-application")
public class BinaryDeployApplicationMojo extends BaseApplicationMojo {
	@Parameter(property = PREFIX + "binary", defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
	protected File binary;

	@Parameter(property = PREFIX + "repo", defaultValue = "src/main/openshift")
	protected File repo;

	@Override
	protected void doExecute(final IOpenShiftConnection connection,
			final IApplication application) throws MojoExecutionException {
		try {
			BinaryDeployer bd = new BinaryDeployer(application);
			String cartridgeName = application.getCartridge().getName();
			bd.addFile(binary, "dependencies/" + cartridgeName.split("-")[0]
					+ "/deployments/ROOT.war");
			if (repo.exists())
				bd.addRecursive(repo, "repo");
			bd.disconnect();

		} catch (URISyntaxException e) {
			throw new MojoExecutionException(e.getMessage(), e);

		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);

		} catch (JSchException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		getLog().info(
				"Application redeployed, you can access it on "
						+ application.getApplicationUrl());
	}
}
