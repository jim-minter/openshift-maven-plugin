package com.worldline.openshift.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;

class BinaryDeployer {
	private final ChannelExec channel;
	private final InputStream is;
	private final Session session;
	private final TarArchiveOutputStream tar;

	public BinaryDeployer(final IApplication application) throws IOException,
			JSchException, URISyntaxException {
		final URI uri = new URI(application.getSshUrl());

		final JSch jsch = new JSch();
		loadKeys(jsch);

		session = jsch.getSession(uri.getUserInfo(), uri.getHost());
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();

		channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand("/usr/bin/oo-binary-deploy");
		final OutputStream os = channel.getOutputStream();
		is = channel.getInputStream();

		channel.connect();

		final GzipCompressorOutputStream gz = new GzipCompressorOutputStream(os);
		tar = new TarArchiveOutputStream(gz);
	}

	protected void loadKeys(final JSch jsch) throws JSchException {
		final File sshRoot = new File(System.getProperty("user.home")
				+ File.separator + ".ssh");
		for (File f : sshRoot.listFiles()) {
			if (new File(f.getPath() + ".pub").exists()
					&& !KeyPair.load(jsch, f.getPath()).isEncrypted())
				jsch.addIdentity(f.getPath());
		}
	}

	public void addFile(final File file, final String fileName)
			throws IOException {
		final TarArchiveEntry e = new TarArchiveEntry(file, fileName);
		tar.putArchiveEntry(e);
		IOUtils.copy(new FileInputStream(file), tar);
		tar.closeArchiveEntry();
	}

	public void addRecursive(final File dir, final String root)
			throws IOException {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				addRecursive(f, root + "/" + f.getName());
			} else {
				addFile(f, root + "/" + f.getName());
			}
		}
	}

	public void disconnect() throws IOException {
		tar.close();
		IOUtils.copy(is, System.err);

		channel.disconnect();
		session.disconnect();
	}
}
