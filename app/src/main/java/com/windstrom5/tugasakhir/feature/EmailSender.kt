package com.windstrom5.tugasakhir.feature

import com.windstrom5.tugasakhir.BuildConfig
import java.util.Properties
import javax.mail.*
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailSender {
    companion object {
        fun sendEmail(receiverEmail: String, subject: String, message: String) {
            try {
                val senderEmail = BuildConfig.Email
                val senderPassword = BuildConfig.Email_Password
                val properties: Properties = System.getProperties()
                properties.setProperty("mail.transport.protocol", "smtp")
                properties.setProperty("mail.host", "smtp.gmail.com")
                properties["mail.smtp.host"] = "smtp.gmail.com"
                properties["mail.smtp.port"] = "465"
                properties["mail.smtp.socketFactory.fallback"] = "false"
                properties.setProperty("mail.smtp.quitwait", "false")
                properties["mail.smtp.socketFactory.port"] = "465"
                properties["mail.smtp.starttls.enable"] = "true"
                properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                properties["mail.smtp.ssl.enable"] = "true"
                properties["mail.smtp.auth"] = "true"

                val session: Session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                        return javax.mail.PasswordAuthentication(senderEmail, senderPassword)
                    }
                })

                val mimeMessage = MimeMessage(session)
                mimeMessage.addRecipient(Message.RecipientType.TO, InternetAddress(receiverEmail))
                mimeMessage.subject = subject
                mimeMessage.setText(message)

                val thread = Thread {
                    try {
                        Transport.send(mimeMessage)
                    } catch (e: MessagingException) {
                        e.printStackTrace()
                    }
                }
                thread.start()
            } catch (e: AddressException) {
                e.printStackTrace()
            } catch (e: MessagingException) {
                e.printStackTrace()
            }
        }
    }
}
