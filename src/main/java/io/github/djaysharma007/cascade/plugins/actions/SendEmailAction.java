package io.github.djaysharma007.cascade.plugins.actions;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Sends an email notification via SMTP.
 * Never stores passwords directly; reads credentials from an environment variable at runtime.
 */
@Plugin(id = "send-email", displayName = "Send email", category = PluginCategory.ACTION)
public final class SendEmailAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(SendEmailAction.class);
    private static final int DEFAULT_SMTP_PORT = 587;
    private static final int TIMEOUT_MS = 10_000;

    private final String smtpHost;
    private final int smtpPort;
    private final String username;
    private final String passwordEnvVar;
    private final String to;
    private final String subject;
    private final String body;

    public SendEmailAction(
            String smtpHost,
            Integer smtpPort,
            String username,
            String passwordEnvVar,
            String to,
            String subject,
            String body
    ) {
        Objects.requireNonNull(smtpHost, "smtpHost must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(body, "body must not be null");

        this.smtpHost = smtpHost.trim();
        this.smtpPort = smtpPort != null && smtpPort > 0 ? smtpPort : DEFAULT_SMTP_PORT;
        this.username = username != null ? username.trim() : "";
        this.passwordEnvVar = passwordEnvVar != null ? passwordEnvVar.trim() : "";
        this.to = to.trim();
        this.subject = subject;
        this.body = body;
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String resolvedTo = TemplateResolver.resolve(to, event, context, null);
        String resolvedSubject = TemplateResolver.resolve(subject, event, context, null);
        String resolvedBody = TemplateResolver.resolve(body, event, context, null);

        if (!resolvedTo.contains("@")) {
            return new ExecutionResult.Failed(new Failure.Expected("Invalid email recipient address: " + resolvedTo));
        }

        String password = null;
        if (!passwordEnvVar.isBlank()) {
            password = System.getenv(passwordEnvVar);
            if (password == null) {
                log.warn("Environment variable {} for SMTP password was not found", passwordEnvVar);
            }
        }

        return deliverEmail(resolvedTo, resolvedSubject, resolvedBody, password);
    }

    private ExecutionResult deliverEmail(String recipient, String sub, String content, String pass) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(smtpHost, smtpPort), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                String banner = reader.readLine();
                if (banner == null || !banner.startsWith("220")) {
                    return new ExecutionResult.Failed(new Failure.Transient("SMTP server returned unexpected greeting: " + banner));
                }

                sendCommand(writer, "EHLO cascade.local");
                consumeResponse(reader);

                String sender = !username.isBlank() ? username : "cascade@localhost";
                sendCommand(writer, "MAIL FROM:<" + sender + ">");
                String mailResp = reader.readLine();
                if (mailResp == null || !mailResp.startsWith("250")) {
                    return new ExecutionResult.Failed(new Failure.Transient("MAIL FROM rejected: " + mailResp));
                }

                sendCommand(writer, "RCPT TO:<" + recipient + ">");
                String rcptResp = reader.readLine();
                if (rcptResp == null || !rcptResp.startsWith("250")) {
                    return new ExecutionResult.Failed(new Failure.Expected("RCPT TO rejected for recipient: " + rcptResp));
                }

                sendCommand(writer, "DATA");
                String dataResp = reader.readLine();
                if (dataResp == null || !dataResp.startsWith("354")) {
                    return new ExecutionResult.Failed(new Failure.Transient("DATA command rejected: " + dataResp));
                }

                writer.write("Subject: " + sub + "\r\n");
                writer.write("To: " + recipient + "\r\n");
                writer.write("From: " + sender + "\r\n");
                writer.write("\r\n");
                writer.write(content + "\r\n");
                writer.write(".\r\n");
                writer.flush();

                String sendResp = reader.readLine();
                if (sendResp == null || !sendResp.startsWith("250")) {
                    return new ExecutionResult.Failed(new Failure.Transient("Message body rejected: " + sendResp));
                }

                sendCommand(writer, "QUIT");
                log.info("Email successfully sent to {} with subject: {}", recipient, sub);
                return new ExecutionResult.Success(Map.of(
                        "recipient", recipient,
                        "subject", sub
                ));
            }
        } catch (Exception e) {
            log.warn("SMTP delivery to {}:{} failed: {}", smtpHost, smtpPort, e.getMessage());
            return new ExecutionResult.Failed(new Failure.Transient("SMTP delivery failed: " + e.getMessage()));
        }
    }

    private void sendCommand(BufferedWriter writer, String cmd) throws Exception {
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private void consumeResponse(BufferedReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }
}
