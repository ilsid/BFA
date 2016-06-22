package com.ilsid.bfa.persistence.filesystem;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * Listens updates of shared FS repository made by other nodes in clustered environment.
 * 
 * @author illia.sydorovych
 *
 */
class RepositoryUpdateListener {

	private static final String READ_VERSION_ERR_MSG = "Update Listener: Failed to read version";

	private static final Object VERSION_UPDATE_LOCK = new Object();

	private static boolean isStarted;

	private static String currentVersion;

	private static FilesystemRepository repository;

	static void start(FilesystemRepository fsRepository, String initialVersion) {
		if (!isStarted) {

			currentVersion = initialVersion;
			repository = fsRepository;
			DynamicClassLoader.addPermanentReloadListener(new CurrentVersionUpdater());

			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.scheduleAtFixedRate(new VersionUpdateListener(), 0, 10, TimeUnit.SECONDS);

			isStarted = true;
		}
	}

	/*
	 * Returns null on exception
	 */
	private static String readVersion() {
		String version;
		try {
			version = repository.readVersion();
		} catch (IOException | RuntimeException e) {
			repository.getLogger().error(READ_VERSION_ERR_MSG, e);
			return null;
		}

		return version;
	}

	private static class VersionUpdateListener implements Runnable {

		public void run() {
			String version = readVersion();

			if (version != null) {
				synchronized (VERSION_UPDATE_LOCK) {
					if (!currentVersion.equals(version)) {
						DynamicClassLoader.reloadClasses();
						currentVersion = version;
					}
				}
			}
		}

	}

	private static class CurrentVersionUpdater implements DynamicClassLoader.ReloadListener {

		public void execute() {
			synchronized (VERSION_UPDATE_LOCK) {
				String version = readVersion();
				if (version != null) {
					currentVersion = version;
				}
			}
		}

	}

}
