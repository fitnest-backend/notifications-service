package az.fitnest.notifications.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${SPRING_MAIL_FROM:fitnestazerbaijan@gmail.com}")
    private String fromAddress;

    @Override
    @Async("taskExecutor")
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.info("Sending HTML email to: {} with subject: {} using template: {}", to, subject, templateName);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent HTML email to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}, subject: {}. Check SMTP credentials and connection.", to, subject, e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            log.info("Sending simple email to: {} with subject: {}", to, subject);
            mailSender.send(message);
            log.info("Successfully sent simple email to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}, subject: {}. Check SMTP credentials and connection.", to, subject, e);
        }
    }
}
