package com.iorga.webappwatcher.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class Tester implements Runnable {

	private final String principal;


	public Tester(final String principal) {
		this.principal = principal;
	}

	public void request(final long duration, final boolean useCpu) {
		try {
			System.out.println("Request launch principal="+principal+"&duration="+duration+"&useCpu="+useCpu);
			final URL url = new URL("http://localhost:8080/webappwatcher-web/test.xhtml?principal="+principal+"&duration="+duration+"&useCpu="+useCpu);
			final InputStream inputStream = url.openConnection().getInputStream();
			final byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				System.out.println(new String(buffer, 0, bytesRead));
			}
			System.out.println("Request OK principal="+principal+"&duration="+duration+"&useCpu="+useCpu);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void sleep(final long duration) {
		try {
			Thread.sleep(duration * 1000);
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(final String[] args) {
		final ThreadGroup threadGroup = new ThreadGroup("tester");
		threadGroup.setDaemon(false);
		new Thread(threadGroup, new Tester("user1") {
			@Override
			public void run() {
				request(30, true);
				sleep(4);
				request(15, true);
			}
		}).start();
		new Thread(threadGroup, new Tester("user2") {
			@Override
			public void run() {
				request(10, false);
				sleep(4);
				request(15, false);
			}
		}).start();
		new Thread(threadGroup, new Tester("user3") {
			@Override
			public void run() {
				sleep(10);
				request(15, true);
				sleep(4);
				request(15, true);
			}
		}).start();
	}

}
