package com.iorga.webappwatcher.watcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.iorga.webappwatcher.event.RetentionLogWritingEvent;
import com.iorga.webappwatcher.util.PatternDuration;

public class RetentionLogWritingWatcher {
	private final static Logger log = LoggerFactory.getLogger(RetentionLogWritingWatcher.class);

	private final Map<String, Date> lastEvents = Maps.newHashMap();

	private List<PatternDuration> writingEventsCooldown = new ArrayList<PatternDuration>();
	{
		writingEventsCooldown.add(new PatternDuration(Pattern.compile(".*RequestLogFilter#.*"), -1)); // Disable sending mail for manual writing (request "writeRetentionLog")
		writingEventsCooldown.add(new PatternDuration(Pattern.compile(".*WriteAllRequestsWatcher#.*"), -1)); // Disable sending mail for every request
		writingEventsCooldown.add(new PatternDuration(Pattern.compile(".*"), 30 * 60));	// By default, all other event writings must send a mail every 30mn
	}

	private String mailSmtpHost;
	private Integer mailSmtpPort = 25;
	private Boolean mailSmtpAuth;
	private String mailSmtpUsername;
	private String mailSmtpPassword;
	private String mailSmtpSecurityType;

	private String mailFrom;
	private String mailTo;

	@Subscribe
	public void onEvent(final RetentionLogWritingEvent event) throws ExecutionException {
		final String eventName = event.getSource()+"#"+event.getReason();
		for (final PatternDuration writingEventCooldown : getWritingEventsCooldown()) {
			if (writingEventCooldown.getPattern().matcher(eventName).matches()) {
				// Checking the cooldown
				final Integer cooldown = writingEventCooldown.getDuration();
				if (cooldown == -1) {
					// disable sending mail for that event
				} else {
					final Date lastEventDate = lastEvents.get(eventName);
					if (lastEventDate == null || (new Date().getTime() - lastEventDate.getTime() > cooldown * 1000)) {
						// time over, let's send a new mail
						lastEvents.put(eventName, new Date());
						sendMailForEvent(event);
					} else {
						// cooldown not reached, do nothing
					}
				}
				break; // stops on first match
			}
		}
	}

	private void sendMailForEvent(final RetentionLogWritingEvent event) {
		log.info("Trying to send a mail for event "+event);

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (StringUtils.isEmpty(getMailSmtpHost()) || getMailSmtpPort() == null) {
					// no configuration defined, exiting
					log.error("Either SMTP host or port was not defined, not sending that mail");
					return;
				}
				// example from http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
				final Properties props = new Properties();
				props.put("mail.smtp.host", getMailSmtpHost());
				props.put("mail.smtp.port", getMailSmtpPort());
				final Boolean auth = getMailSmtpAuth();
				Authenticator authenticator = null;
				if (BooleanUtils.isTrue(auth)) {
					props.put("mail.smtp.auth", "true");
					authenticator = new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(getMailSmtpUsername(), getMailSmtpPassword());
						}
					};
				}
				if (StringUtils.equalsIgnoreCase(getMailSmtpSecurityType(), "SSL")) {
					props.put("mail.smtp.socketFactory.port", getMailSmtpPort());
					props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				} else if (StringUtils.equalsIgnoreCase(getMailSmtpSecurityType(), "TLS")) {
					props.put("mail.smtp.starttls.enable", "true");
				}

				final Session session = Session.getDefaultInstance(props, authenticator);

				try {
					final MimeMessage message = new MimeMessage(session);
					message.setFrom(new InternetAddress(getMailFrom()));
					message.setRecipients(RecipientType.TO, InternetAddress.parse(getMailTo()));
					message.setSubject(event.getReason());
					if (event.getContext() != null) {
						final StringBuilder contextText = new StringBuilder();
						for (final Entry<String, Object> contextEntry : event.getContext().entrySet()) {
							contextText.append(contextEntry.getKey()).append(" = ").append(contextEntry.getValue()).append("\n");
						}
						message.setText(contextText.toString());
					} else {
						message.setText("Context null");
					}

					Transport.send(message);
				} catch (final MessagingException e) {
					log.error("Problem while sending a mail", e);
				}
			}
		}, RetentionLogWritingWatcher.class.getSimpleName()+":sendMailer").start();	// send mail in an async way
	}

	/// Getter & Setters ///
	///////////////////////
	public List<PatternDuration> getWritingEventsCooldown() {
		return writingEventsCooldown;
	}

	public void setWritingEventsCooldown(final List<PatternDuration> writingEventsCooldown) {
		this.writingEventsCooldown = writingEventsCooldown;
	}

	public String getMailSmtpSecurityType() {
		return mailSmtpSecurityType;
	}

	public void setMailSmtpSecurityType(final String mailSecurityType) {
		this.mailSmtpSecurityType = mailSecurityType;
	}

	public String getMailSmtpHost() {
		return mailSmtpHost;
	}

	public void setMailSmtpHost(final String mailSmtpHost) {
		this.mailSmtpHost = mailSmtpHost;
	}

	public Integer getMailSmtpPort() {
		return mailSmtpPort;
	}

	public void setMailSmtpPort(final Integer mailSmtpPort) {
		this.mailSmtpPort = mailSmtpPort;
	}

	public Boolean getMailSmtpAuth() {
		return mailSmtpAuth;
	}

	public void setMailSmtpAuth(final Boolean mailSmtpAuth) {
		this.mailSmtpAuth = mailSmtpAuth;
	}

	public String getMailSmtpUsername() {
		return mailSmtpUsername;
	}

	public void setMailSmtpUsername(final String mailSmtpUsername) {
		this.mailSmtpUsername = mailSmtpUsername;
	}

	public String getMailSmtpPassword() {
		return mailSmtpPassword;
	}

	public void setMailSmtpPassword(final String mailSmtpPassword) {
		this.mailSmtpPassword = mailSmtpPassword;
	}

	public String getMailFrom() {
		return mailFrom;
	}

	public void setMailFrom(final String mailFrom) {
		this.mailFrom = mailFrom;
	}

	public String getMailTo() {
		return mailTo;
	}

	public void setMailTo(final String mailTo) {
		this.mailTo = mailTo;
	}

}
