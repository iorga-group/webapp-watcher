package com.iorga.webappwatcher.analyzer.ws.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextParams;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FileMetadata;

@Path("/session/uploadedFiles")
public class UploadedFilesWS {
	@Inject
	private JsonWriter jsonWriter;

	@Inject
	private UploadedFiles uploadedFiles;

	@ContextParams(
		@ContextParam(name = "files", value = List.class, parameterizedArguments = FileMetadata.class)
	)
	public static class FilesTemplate {
		List<FileMetadataTemplate> files;

		@ContextParam(FileMetadata.class)
		public static class FileMetadataTemplate {
			String name;
			static final String delete_type = "DELETE";

			public static long getSize(final FileMetadata fileMetadata) {
				return fileMetadata.getFile().length();
			}
			public static String getDelete_url(final FileMetadata fileMetadata) {
				return "api/session/uploadedFiles/"+fileMetadata.getId();
			}
		}
	}
	@POST
	@Path("/")
	public StreamingOutput addFiles(final MultipartFormDataInput input) throws IOException {
		// based on http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-resteasy/
		final List<InputPart> parts = input.getFormDataMap().get("files[]");
		final List<FileMetadata> fileMetadatas = Lists.newArrayList();
		for (final InputPart inputPart : parts) {
			final String fileName = getFileName(inputPart.getHeaders());
			final FileMetadata fileMetadata = uploadedFiles.addFile(fileName, inputPart.getBody(InputStream.class, null));
			fileMetadatas.add(fileMetadata);
		}
		return writeToStreamingOutput(fileMetadatas);
	}

	private String getFileName(final MultivaluedMap<String, String> headers) {
		final String[] contentDisposition = headers.getFirst("Content-Disposition").split(";");

		for (final String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {

				final String[] name = filename.split("=");

				final String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}

	private StreamingOutput writeToStreamingOutput(final Collection<FileMetadata> fileMetadatas) {
		final Map<String, Object> context = Maps.newHashMap();
		context.put("files", fileMetadatas);
		return jsonWriter.writeWithTemplate(FilesTemplate.class, context);
	}

	@GET
	@Path("/")
	public StreamingOutput getFiles() throws IOException {
		final Collection<FileMetadata> fileMetadatas = uploadedFiles.getFiles();
		return writeToStreamingOutput(fileMetadatas);
	}

	@DELETE
	@Path("/{fileId}")
	public void deleteFile(@PathParam("fileId") final String fileId) {
		uploadedFiles.deleteFile(fileId);
	}
}
