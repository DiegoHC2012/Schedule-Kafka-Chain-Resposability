package com.broker.service;

import com.broker.model.RetryJob;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.success-recipient}")
    private String successRecipient;

    @Value("${app.email.failure-recipient}")
    private String failureRecipient;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ─── Domain metadata ─────────────────────────────────────────────────────

    private record DomainMeta(String label, String icon, String accentColor, String accentLight) {}

    private DomainMeta meta(String topic) {
        return switch (topic) {
            case "order_retry_jobs"   -> new DomainMeta("Orden",    "📦", "#7c3aed", "#ede9fe");
            case "product_retry_jobs" -> new DomainMeta("Producto", "🛍",  "#d97706", "#fef3c7");
            default                   -> new DomainMeta("Pago",     "💳", "#2563eb", "#dbeafe");
        };
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void sendSuccessEmail(RetryJob retryJob, Map<String, Object> data) {
        DomainMeta m = meta(retryJob.getTopic());
        String subject = m.icon + " " + m.label + " procesado correctamente";
        String html = buildSuccessHtml(retryJob, data, m);
        send(successRecipient, subject, html);
    }

    public void sendFailureEmail(RetryJob retryJob, String errorMessage) {
        DomainMeta m = meta(retryJob.getTopic());
        String subject = "⚠️ Fallo en procesamiento de " + m.label.toLowerCase();
        String html = buildFailureHtml(retryJob, errorMessage, m);
        send(failureRecipient, subject, html);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    private String buildSuccessHtml(RetryJob job, Map<String, Object> data, DomainMeta m) {
        String name        = str(data, "name",        "—");
        String description = str(data, "description", "—");
        String price       = str(data, "price",       "—");
        String quantity    = str(data, "quantity",    "—");
        String category    = str(data, "category",    "—");
        String brand       = str(data, "brand",       "—");
        String image       = str(data, "image",       "");
        String itemId      = str(data, "id",          job.getId().toString());

        String imageRow = image.isBlank() ? "" : """
            <tr>
              <td style="padding:0 0 20px 0;text-align:center;">
                <img src="%s" alt="product" style="max-width:200px;max-height:200px;border-radius:8px;object-fit:cover;"/>
              </td>
            </tr>
            """.formatted(escHtml(image));

        return baseLayout(m.accentColor, m.accentLight, """
            <tr>
              <td style="padding:32px 40px 8px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td>
                      <span style="display:inline-block;background:%s;color:#fff;font-size:11px;font-weight:700;
                                   letter-spacing:1px;text-transform:uppercase;padding:4px 10px;border-radius:20px;">
                        ✓ &nbsp;Completado
                      </span>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding-top:16px;">
                      <h1 style="margin:0;font-size:22px;font-weight:700;color:#0f172a;line-height:1.3;">
                        %s %s procesado exitosamente
                      </h1>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            %s
            <tr>
              <td style="padding:16px 40px 32px 40px;">
                %s
                %s
              </td>
            </tr>
            """.formatted(
                m.accentColor,
                m.icon, m.label,
                imageRow,
                dataTable(new String[][]{
                    {"ID del item",   itemId},
                    {"Nombre",        name},
                    {"Descripción",   description},
                    {"Precio",        price.equals("—") ? "—" : "$ " + price},
                    {"Cantidad",      quantity},
                    {"Categoría",     category},
                    {"Marca",         brand}
                }, m.accentColor),
                jobInfo(job, m.accentLight)
            )
        );
    }

    private String buildFailureHtml(RetryJob job, String errorMessage, DomainMeta m) {
        String attempts = job.getAttemptCount() + " / " + job.getMaxAttempts();
        String updatedAt = job.getUpdatedAt() != null ? job.getUpdatedAt().format(FMT) : "—";

        return baseLayout("#dc2626", "#fef2f2", """
            <tr>
              <td style="padding:32px 40px 8px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td>
                      <span style="display:inline-block;background:#dc2626;color:#fff;font-size:11px;font-weight:700;
                                   letter-spacing:1px;text-transform:uppercase;padding:4px 10px;border-radius:20px;">
                        ✗ &nbsp;Fallido
                      </span>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding-top:16px;">
                      <h1 style="margin:0;font-size:22px;font-weight:700;color:#0f172a;line-height:1.3;">
                        %s %s agotó sus reintentos
                      </h1>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 40px 8px 40px;">
                <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;">
                  <p style="margin:0 0 4px 0;font-size:11px;font-weight:700;color:#dc2626;text-transform:uppercase;letter-spacing:0.5px;">
                    Mensaje de error
                  </p>
                  <p style="margin:0;font-size:13px;color:#7f1d1d;font-family:monospace;word-break:break-word;">
                    %s
                  </p>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:16px 40px 32px 40px;">
                %s
                %s
              </td>
            </tr>
            """.formatted(
                m.icon, m.label,
                escHtml(errorMessage),
                dataTable(new String[][]{
                    {"Intentos realizados", attempts},
                    {"Último intento",      updatedAt},
                    {"Topic Kafka",         job.getTopic()}
                }, "#dc2626"),
                jobInfo(job, "#fef2f2")
            )
        );
    }

    private String dataTable(String[][] rows, String accentColor) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                   style="border-collapse:collapse;margin-bottom:20px;">
            """);
        for (int i = 0; i < rows.length; i++) {
            String bg = (i % 2 == 0) ? "#f8fafc" : "#ffffff";
            sb.append("""
                <tr>
                  <td style="padding:10px 14px;background:%s;border-bottom:1px solid #e2e8f0;
                              font-size:12px;color:#64748b;font-weight:600;width:38%%;white-space:nowrap;">
                    %s
                  </td>
                  <td style="padding:10px 14px;background:%s;border-bottom:1px solid #e2e8f0;
                              font-size:13px;color:#0f172a;word-break:break-word;">
                    %s
                  </td>
                </tr>
                """.formatted(bg, escHtml(rows[i][0]), bg, escHtml(rows[i][1])));
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String jobInfo(RetryJob job, String bgColor) {
        String createdAt = job.getCreatedAt() != null ? job.getCreatedAt().format(FMT) : "—";
        return """
            <div style="background:%s;border-radius:8px;padding:14px 16px;margin-top:4px;">
              <p style="margin:0;font-size:11px;color:#64748b;line-height:1.8;">
                <strong style="color:#334155;">Job ID:</strong>&nbsp;%s<br/>
                <strong style="color:#334155;">Topic:</strong>&nbsp;%s<br/>
                <strong style="color:#334155;">Creado:</strong>&nbsp;%s
              </p>
            </div>
            """.formatted(bgColor, job.getId(), escHtml(job.getTopic()), createdAt);
    }

    private String baseLayout(String accentColor, String accentLight, String body) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f1f5f9;padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="580" cellpadding="0" cellspacing="0" border="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 1px 3px rgba(0,0,0,0.08),0 4px 12px rgba(0,0,0,0.05);
                                  max-width:580px;width:100%%;">

                      <!-- Header bar -->
                      <tr>
                        <td style="background:%s;padding:20px 40px;">
                          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td>
                                <span style="font-size:13px;font-weight:700;color:#ffffff;letter-spacing:0.5px;">
                                  ⚡ Broker Retry System
                                </span>
                              </td>
                              <td align="right">
                                <span style="font-size:11px;color:rgba(255,255,255,0.75);">
                                  Chain of Responsibility
                                </span>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- Body -->
                      %s

                      <!-- Footer -->
                      <tr>
                        <td style="padding:20px 40px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                          <p style="margin:0;font-size:11px;color:#94a3b8;text-align:center;line-height:1.6;">
                            Este es un mensaje automático del sistema de reintentos.<br/>
                            Por favor no respondas a este correo.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(accentColor, body);
    }

    private String str(Map<String, Object> data, String key, String fallback) {
        if (data == null) return fallback;
        Object v = data.get(key);
        if (v == null || v.toString().isBlank()) return fallback;
        return v.toString();
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
