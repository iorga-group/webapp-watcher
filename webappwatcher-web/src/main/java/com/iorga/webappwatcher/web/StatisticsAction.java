package com.iorga.webappwatcher.web;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.tukaani.xz.CorruptedInputException;

import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;

@ManagedBean
@ViewScoped
public class StatisticsAction implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final long AGGLOMERATION_DURATION_MILLIS = 10 * 60 * 1000;
	private static final long NULL_AFTER_PROCESS_DATE_DURATION_MILLIS = 45 * 1000;
	private static final long RELEVENT_REQUEST_DURATION_THRESHOLD = 500;

	private List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();

	private static class CSVLine {
		private Date startDate = null;
		private final Set<String> principals = new HashSet<String>();
		private final DescriptiveStatistics durations = new DescriptiveStatistics();
		private final DescriptiveStatistics releventDurations = new DescriptiveStatistics();
	}

	/// Actions ///
	//////////////
	public void extractDurationStats() throws IOException, ClassNotFoundException {
		// based on http://stackoverflow.com/a/9394237/535203
		final FacesContext facesContext = FacesContext.getCurrentInstance();

		final HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		response.reset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
		response.setContentType("text/csv"); // Check http://www.w3schools.com/media/media_mimeref.asp for all types. Use if necessary ServletContext#getMimeType() for auto-detection based on filename.
//		response.setContentLength(contentLength); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
		response.setHeader("Content-Disposition", "attachment; filename=\"extract.csv\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

		final ServletOutputStream outputStream = response.getOutputStream();

		outputStream.println("Start date;End date;Distinct users;Number of requests;Duration;Average;Median;90c;Relevent number of requests;Relevent duration;Relevent Average;Relevent Median;Relevent 90c");

		for (final UploadedFile uploadedFile : uploadedFiles) {
			readEventLogs(outputStream, uploadedFile.getInputstream());
		}

		facesContext.responseComplete();
	}

	/// Events ///
	/////////////
	public void handleFileUpload(final FileUploadEvent event) throws IOException, ClassNotFoundException {
		System.out.println("Handling "+event.getFile().getFileName());
		uploadedFiles.add(event.getFile());
	}

	/// Utils ///
	////////////
	public void readEventLogs(final ServletOutputStream outputStream, final InputStream inputstream) throws IOException, ClassNotFoundException {
		final ObjectInputStream objectInputStream = EventLogManager.readLog(inputstream);
		try {
			CSVLine csvLine = new CSVLine();

			EventLog eventLog;
			RequestEventLog requestEventLog = null;
			try {
				while ((eventLog = readEventLog(objectInputStream)) != null) {
					if (eventLog instanceof RequestEventLog) {
						requestEventLog = (RequestEventLog) eventLog;

						csvLine = readRequestEventLog(requestEventLog, csvLine, outputStream);
					}
				}
			} catch (final EOFException e) {
				// Normal end of the read file
			}
			writeCsvLine(csvLine, requestEventLog, outputStream);
		} finally {
			objectInputStream.close();
		}
	}

	private EventLog readEventLog(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		try {
			return (EventLog) objectInputStream.readObject();
		} catch (final CorruptedInputException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	private CSVLine readRequestEventLog(final RequestEventLog requestEventLog, CSVLine csvLine, final ServletOutputStream outputStream) throws IOException {
		final Date date = requestEventLog.getDate();
		if (csvLine.startDate == null) {
			csvLine.startDate = date;
		} else if(date.getTime() - csvLine.startDate.getTime() > AGGLOMERATION_DURATION_MILLIS) {
			writeCsvLine(csvLine, requestEventLog, outputStream);
			csvLine = new CSVLine();
			csvLine.startDate = date;
		}
		// adding a point in the csv line
		Long durationMillis = requestEventLog.getDurationMillis();
		if (durationMillis == null) {
			System.err.println("Null duration, will treat it as "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+"ms");
			durationMillis = NULL_AFTER_PROCESS_DATE_DURATION_MILLIS;
		}
		csvLine.durations.addValue(durationMillis);
		if (durationMillis > RELEVENT_REQUEST_DURATION_THRESHOLD) {
			csvLine.releventDurations.addValue(durationMillis);
		}
		csvLine.principals.add(requestEventLog.getPrincipal());

		return csvLine;
	}

	private void writeCsvLine(final CSVLine csvLine, final RequestEventLog currentRequestEventLog, final ServletOutputStream outputStream) throws IOException {
		final DescriptiveStatistics durations = csvLine.durations;
		final DescriptiveStatistics releventDurations = csvLine.releventDurations;
		final StringBuilder line = new StringBuilder();
		// Processing "Start date;End date;Distinct users;Number of requests;Duration;Average;Median;90c;Relevent number of requests;Relevent duration;Relevent Average;Relevent Median;Relevent 90c"
		final Date currentRequestDate = currentRequestEventLog.getDate();
		line.append(dateFormatter.format(csvLine.startDate)).append(";")
			.append(dateFormatter.format(currentRequestDate != null ? currentRequestDate : csvLine.startDate)).append(";")
			.append(csvLine.principals.size()).append(";")
			.append(durations.getN()).append(";")
			.append((int)durations.getSum()).append(";")
			.append((int)durations.getMean()).append(";")
			.append((int)durations.getPercentile(50)).append(";")
			.append((int)durations.getPercentile(90)).append(";")
			.append(releventDurations.getN()).append(";")
			.append((int)releventDurations.getSum()).append(";")
			.append((int)releventDurations.getMean()).append(";")
			.append((int)releventDurations.getPercentile(50)).append(";")
			.append((int)releventDurations.getPercentile(90)).append(";");

		outputStream.println(line.toString());
	}

	/// Getters & Setters ///
	public List<UploadedFile> getUploadedFiles() {
		return uploadedFiles;
	}

	public void setUploadedFiles(final List<UploadedFile> uploadedFiles) {
		this.uploadedFiles = uploadedFiles;
	}
}
