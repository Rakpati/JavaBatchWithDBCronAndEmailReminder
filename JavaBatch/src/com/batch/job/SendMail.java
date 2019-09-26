package com.batch.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import java.util.TreeSet;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class SendMail {

	void mailSender(TreeSet<String> name, TreeSet<String> email, TreeSet<Integer> days) {

		final Properties prop = System.getProperties();

		try {
			prop.load(new FileInputStream(new File("resources/mail.properties")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Session session = Session.getDefaultInstance(prop,
				new Authenticator() {

			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(
						prop.getProperty("mail.user"), prop.getProperty("mail.passwd"));
			}
		});

		try{
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(prop.getProperty("mail.user")));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.last()));
			message.setSubject(prop.getProperty("mail.subject"));

			BodyPart body = new MimeBodyPart();

			VelocityEngine ve = new VelocityEngine();
			ve.init();
			Template t = null;
			if(days.last() == 3 || days.last() == 7){
				t = ve.getTemplate("template/reminderTemplate.vm");
			}else if(days.last() == 21) { 
				t = ve.getTemplate("template/finalReminderTemplate.vm");
			}
			VelocityContext context = new VelocityContext();
			context.put("user", name.last());
			StringWriter out = new StringWriter();
			t.merge( context, out );

			body.setContent(out.toString(), "text/html");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(body);

			message.setContent(multipart, "text/html");

			// Send mail
			Transport.send(message);
			System.out.println("Mail sent ");

		}catch(Exception ex){
			ex.printStackTrace();
		}

	}
}