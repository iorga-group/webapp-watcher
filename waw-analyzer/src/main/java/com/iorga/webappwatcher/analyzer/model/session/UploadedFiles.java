package com.iorga.webappwatcher.analyzer.model.session;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.CorruptedInputException;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.analyzer.ws.session.UploadedFilesWS;
import com.iorga.webappwatcher.eventlog.EventLog;

@SessionScoped
public class UploadedFiles implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(UploadedFiles.class);

	private final Map<String, FileMetadata> files = Maps.newHashMap();
	private int nextId = 0;

	@Inject
	private @FilesChanged Event<UploadedFiles> filesChangedEvent;

	public static class FileMetadata implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String id;
		private final String name;
		private final File file;

		public FileMetadata(final String name, final File content, final int id) {
			this.id = ""+id;
			this.name = name;
			this.file = content;
		}

		public String getName() {
			return name;
		}
		public File getFile() {
			return file;
		}
		public String getId() {
			return id;
		}
	}

	public abstract static class FileMetadataReader {
		public void readFileMetadatas(final Iterable<FileMetadata> fileMetadatas) throws IOException, ClassNotFoundException {
			for (final FileMetadata fileMetadata : fileMetadatas) {
				handleUploadedFileInputStream(new FileInputStream(fileMetadata.getFile()), fileMetadata.getName());
			}
		}

		protected void handleUploadedFileInputStream(final InputStream uploadedFileinputstream, final String uploadedFileName) throws IOException, ClassNotFoundException {
			if (uploadedFileName.endsWith(".zip")) {
				// it's a zip, we must iterate on each files inside
				// we will first copy the zip file in order to access it via ZipFile
				final File tempZipFile = File.createTempFile("waw", ".zip");
				IOUtils.copy(uploadedFileinputstream, new FileOutputStream(tempZipFile));
				try {
					final ZipFile zipFile = new ZipFile(tempZipFile);
					// sort the files in order to read them ascending
					final List<ZipArchiveEntry> sortedZipArchiveEntries = Ordering.from(new Comparator<ZipArchiveEntry>() {
						@Override
						public int compare(final ZipArchiveEntry o1, final ZipArchiveEntry o2) {
							return o1.getName().compareTo(o2.getName());
						}
					}).sortedCopy(new Iterable<ZipArchiveEntry>() {
						@Override
						public Iterator<ZipArchiveEntry> iterator() {
							return Iterators.forEnumeration(zipFile.getEntries());
						}
					});
					for (final ZipArchiveEntry zipArchiveEntry : sortedZipArchiveEntries) {
						handleInputStreamAndFileName(zipFile.getInputStream(zipArchiveEntry), zipArchiveEntry.getName());
					}
				} finally {
					tempZipFile.delete();
				}
			} else {
				handleInputStreamAndFileName(uploadedFileinputstream, uploadedFileName);
			}
		}

		protected void handleInputStreamAndFileName(final InputStream uploadedFileinputstream, final String uploadedFileName) throws IOException, ClassNotFoundException, FileNotFoundException {
			handleObjectInputStream(EventLogManager.readLog(uploadedFileinputstream, uploadedFileName));
		}

		protected void handleObjectInputStream(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
			try {
				EventLog eventLog;
				try {
					while ((eventLog = readEventLog(objectInputStream)) != null) {
						handleEventLog(eventLog);
					}
				} catch (final EOFException e) {
					// Normal end of the read file
				}
			} finally {
				objectInputStream.close();
			}
		}

		protected EventLog readEventLog(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
			try {
				return (EventLog) objectInputStream.readObject();
			} catch (final CorruptedInputException e) {
				log.error("Problem while reading the ObjectInputStream", e);
				return null;
			}
		}

		protected abstract void handleEventLog(EventLog eventLog) throws IOException;
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
	public static @interface FilesChanged {}

	/// Actions ///
	//////////////
	public FileMetadata addFile(final String fileName, final InputStream fileContent) throws IOException {
		final File tempFile = File.createTempFile(UploadedFilesWS.class.getName(), ".temp");
		// copy the stream to a temp file
		IOUtils.copy(fileContent, new FileOutputStream(tempFile));
		final FileMetadata uploadedFile = new FileMetadata(fileName, tempFile, nextId++);
		files.put(uploadedFile.getId(), uploadedFile);
		filesChangedEvent.fire(this);
		return uploadedFile;
	}

	public void deleteFile(final String fileId) {
		final FileMetadata fileMetadata = files.get(fileId);
		deleteFile(fileMetadata);
	}

	public void readFiles(final FileMetadataReader fileMetadataReader) throws IOException, ClassNotFoundException {
		fileMetadataReader.readFileMetadatas(getFiles());
	}

	/// Utils ///
	////////////

	private void deleteFile(final FileMetadata fileMetadata) {
		fileMetadata.file.delete();
		files.remove(fileMetadata.getId());
		filesChangedEvent.fire(this);
	}

	/// Structure ///
	////////////////
	@PreDestroy
	public void dispose() {
		for (final FileMetadata fileMetadata : Lists.newArrayList(files.values())) {
			deleteFile(fileMetadata);
		}
	}

	/// Getters & Setters ///
	////////////////////////
	public Collection<FileMetadata> getFiles() {
		return files.values();
	}
}
