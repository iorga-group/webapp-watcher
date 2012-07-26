package com.iorga.webappwatcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class EventLogPrinter {
	public static void main(final String[] args) throws IOException, ClassNotFoundException {
		final FileInputStream inputStream = new FileInputStream("/home/aogier/Applications/JBoss/5.1.0.GA/bin/webappwatcherlog.3.ser");
		final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		try {
			Object readObject;
			while ((readObject = objectInputStream.readObject()) != null) {
				System.out.println(readObject.toString());
			}
		} finally {
			objectInputStream.close();
		}
	}
}
