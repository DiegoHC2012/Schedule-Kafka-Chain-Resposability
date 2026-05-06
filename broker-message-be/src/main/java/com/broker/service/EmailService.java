package com.broker.service;

import com.broker.dto.event.EmailNotificationEvent;
import com.broker.model.RetryJob;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        private record NotificationMeta(
          String badge,
          String accentColor,
          String accentLight,
          String title,
          String body,
          String highlight,
          int progressStep
        ) {}

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

    public void sendNotificationEmail(EmailNotificationEvent event) {
      String recipient = Objects.requireNonNull(event.recipient(), "recipient is required");
      String subject = Objects.requireNonNullElse(event.subject(), "Notificación del sistema");
      String html = buildNotificationHtml(event);
      send(recipient, subject, html);
      log.info("Notification email sent to {} using template {}", recipient, event.template());
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
        String sender = Objects.requireNonNull(from, "app.email.from is required");
        String recipient = Objects.requireNonNull(to, "recipient is required");
        String safeSubject = Objects.requireNonNull(subject, "subject is required");
        String safeHtml = Objects.requireNonNull(html, "html is required");

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(sender);
        helper.setTo(recipient);
        helper.setSubject(safeSubject);
        helper.setText(safeHtml, true);
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

    private String buildNotificationHtml(EmailNotificationEvent event) {
        String template = Objects.requireNonNullElse(event.template(), "notification");
        Map<String, Object> payload = event.payload();
        NotificationMeta meta = notificationMeta(template, payload);
        String subject = Objects.requireNonNullElse(event.subject(), "Notificación del sistema");

        return marketplaceLayout(meta.accentColor(), """
            <tr>
              <td style="padding:32px 40px 10px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td>
                      <span style="display:inline-block;background:%s;color:#fff;font-size:11px;font-weight:700;
                                   letter-spacing:1px;text-transform:uppercase;padding:4px 10px;border-radius:20px;">
                        %s
                      </span>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding-top:16px;">
                      <h1 style="margin:0;font-size:22px;font-weight:700;color:#0f172a;line-height:1.3;">
                        %s
                      </h1>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding-top:10px;">
                      <p style="margin:0;font-size:14px;color:#475569;line-height:1.8;">
                        %s
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            %s
            %s
            <tr>
              <td style="padding:0 40px 20px 40px;">
                <div style="background:%s;border-radius:16px;padding:16px 18px;">
                  <p style="margin:0;font-size:13px;color:#0f172a;line-height:1.7;">
                    %s
                  </p>
                </div>
              </td>
            </tr>
            <tr>
              <td style="padding:0 40px 32px 40px;">
                %s
              </td>
            </tr>
            """.formatted(
                meta.accentColor(),
                escHtml(meta.badge()),
                escHtml(subject),
                escHtml(meta.body()),
                productSpotlight(payload, meta.accentColor(), meta.accentLight()),
                progressTracker(meta.accentColor(), meta.progressStep()),
                meta.accentLight(),
                escHtml(meta.highlight()),
                dataTable(notificationRows(template, payload), meta.accentColor())
            )
        );
    }

    private NotificationMeta notificationMeta(String template, Map<String, Object> payload) {
        return switch (template) {
            case "payment-received" -> new NotificationMeta(
                    "Pago acreditado",
                    "#00a650",
                    "#edf9f1",
                    "Recibimos tu pago",
                    "Tu compra ya quedó confirmada. Estamos preparando tu pedido para que salga rumbo a tu domicilio cuanto antes.",
                    "Cada vez estás más cerca de recibir tu producto.",
                    2
            );
            case "shipment-confirmation" -> new NotificationMeta(
                    "En camino",
                    "#2968c8",
                    "#eef5ff",
                    "Tu pedido ya va en camino",
                    "Tu compra dejó la etapa de preparación y pronto la tendrás contigo. Te avisaremos si hay una nueva novedad en el envío.",
                    "Ya queda menos para estrenar tu compra.",
                    3
            );
            case "payment-pending" -> new NotificationMeta(
                    "Pago en revisión",
                    "#f59e0b",
                    "#fff7e8",
                    "Estamos validando tu pago",
                    "Recibimos tu intento de pago y lo estamos revisando. En cuanto quede acreditado, te enviaremos una confirmación.",
                    "Tu pedido sigue reservado mientras confirmamos el pago.",
                    1
            );
            case "order-status-changed" -> orderStatusMeta(payload);
            default -> new NotificationMeta(
                    "Actualización",
                    "#0f766e",
                    "#ecfeff",
                    "Hay novedades sobre tu compra",
                    "Registramos una actualización en tu pedido. A continuación te compartimos el resumen más reciente.",
                    "Seguiremos avisándote cada vez que tu compra avance de etapa.",
                    1
            );
        };
    }

    private NotificationMeta orderStatusMeta(Map<String, Object> payload) {
        String status = payloadValue(payload, "status", "");
        return switch (status) {
            case "PAGADO" -> new NotificationMeta(
                    "Pago confirmado",
                    "#00a650",
                    "#edf9f1",
                    "Tu compra ya fue confirmada",
                    "Confirmamos el pago de tu pedido y ya estamos coordinando la siguiente etapa para entregarlo.",
                    "Cada vez estás más cerca de recibir tu producto.",
                    2
            );
            case "EN_PROCESO" -> new NotificationMeta(
                    "Preparando pedido",
                    "#7c3aed",
                    "#f4efff",
                    "Estamos preparando tu pedido",
                    "Tu compra está en preparación. Estamos dejando todo listo para que avance a despacho lo antes posible.",
                    "Tu pedido ya está pasando por la etapa final antes del envío.",
                    2
            );
            case "CANCELADA" -> new NotificationMeta(
                    "Pedido cancelado",
                    "#dc2626",
                    "#fef2f2",
                    "Actualizamos el estado de tu pedido",
                    "Tu compra fue cancelada. Si necesitas ayuda, revisa el detalle del pedido o comunícate con soporte.",
                    "Si esta cancelación no era esperada, conviene revisar el pedido cuanto antes.",
                    0
            );
            case "PENDIENTE_PAGO" -> new NotificationMeta(
                    "Pago pendiente",
                    "#f59e0b",
                    "#fff7e8",
                    "Tu pago sigue pendiente",
                    "Todavía no se acreditó el pago de tu compra. Cuando eso ocurra, retomaremos la preparación del pedido automáticamente.",
                    "Apenas el pago quede confirmado, avanzaremos con tu pedido.",
                    1
            );
            case "CREADA" -> new NotificationMeta(
                    "Pedido recibido",
                    "#2968c8",
                    "#eef5ff",
                    "Recibimos tu pedido",
                    "Tu compra ya quedó registrada en el sistema. El próximo paso es la confirmación del pago.",
                    "Tu pedido ya está creado y listo para avanzar.",
                    1
            );
            default -> new NotificationMeta(
                    "Actualización",
                    "#0f766e",
                    "#ecfeff",
                    "Actualizamos tu pedido",
                    "Tu compra registró una novedad. Te dejamos el detalle más reciente abajo.",
                    "Seguiremos enviándote novedades a medida que el pedido avance.",
                    1
            );
        };
    }

    private String productSpotlight(Map<String, Object> payload, String accentColor, String accentLight) {
        String productName = payloadValue(payload, "productName", payloadValue(payload, "itemsSummary", "Tu compra"));
        String itemsSummary = payloadValue(payload, "itemsSummary", productName);
        String productImage = payloadValue(payload, "productImage", "");
        String productQuantity = payloadValue(payload, "productQuantity", "");
        String totalItems = payloadValue(payload, "totalItems", "");

        String media = productImage.isBlank()
                ? """
                    <td width="160" valign="top" style="padding:20px;">
                      <div style="height:160px;border-radius:18px;background:%s;display:flex;align-items:center;justify-content:center;text-align:center;">
                        <span style="font-size:54px;line-height:1;">📦</span>
                      </div>
                    </td>
                    """.formatted(accentLight)
                : """
                    <td width="160" valign="top" style="padding:20px;">
                      <img src="%s" alt="%s" style="display:block;width:160px;height:160px;border-radius:18px;object-fit:cover;"/>
                    </td>
                    """.formatted(escHtml(productImage), escHtml(productName));

        String quantityLine = productQuantity.isBlank()
                ? ""
                : "<p style=\"margin:14px 0 0 0;font-size:13px;color:#475569;\">Cantidad destacada: <strong>" + escHtml(productQuantity) + "</strong></p>";
        String itemsLine = totalItems.isBlank()
                ? ""
                : "<p style=\"margin:8px 0 0 0;font-size:13px;color:#475569;\">Unidades en el pedido: <strong>" + escHtml(totalItems) + "</strong></p>";

        return """
            <tr>
              <td style="padding:0 40px 22px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="border:1px solid #e2e8f0;border-radius:24px;background:#ffffff;">
                  <tr>
                    %s
                    <td valign="top" style="padding:22px 22px 22px 0;">
                      <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:%s;">Producto destacado</p>
                      <h2 style="margin:10px 0 8px 0;font-size:22px;line-height:1.3;color:#0f172a;">%s</h2>
                      <p style="margin:0;font-size:14px;color:#334155;line-height:1.8;">%s</p>
                      %s
                      %s
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            """.formatted(
                media,
                accentColor,
                escHtml(productName),
                escHtml(itemsSummary),
                quantityLine,
                itemsLine
        );
    }

    private String progressTracker(String accentColor, int progressStep) {
        if (progressStep <= 0) {
            return "";
        }

        return """
            <tr>
              <td style="padding:0 40px 22px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    %s
                    %s
                    %s
                  </tr>
                </table>
              </td>
            </tr>
            """.formatted(
                progressStepCell("Pedido", progressStep >= 1, accentColor),
                progressStepCell("Pago", progressStep >= 2, accentColor),
                progressStepCell("Entrega", progressStep >= 3, accentColor)
        );
    }

    private String progressStepCell(String label, boolean active, String accentColor) {
        String circleBackground = active ? accentColor : "#e2e8f0";
        String circleColor = active ? "#ffffff" : "#64748b";
        String lineBackground = active ? accentColor : "#e2e8f0";

        return """
            <td width="33.33%%" valign="top">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <td align="center">
                    <div style="display:inline-block;min-width:36px;height:36px;line-height:36px;border-radius:999px;background:%s;color:%s;font-size:13px;font-weight:700;">
                      •
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="padding-top:10px;text-align:center;">
                    <span style="font-size:12px;font-weight:700;color:#334155;letter-spacing:0.2px;">%s</span>
                  </td>
                </tr>
                <tr>
                  <td style="padding-top:10px;">
                    <div style="height:4px;border-radius:999px;background:%s;"></div>
                  </td>
                </tr>
              </table>
            </td>
            """.formatted(circleBackground, circleColor, escHtml(label), lineBackground);
    }

    private String[][] notificationRows(String template, Map<String, Object> payload) {
        List<String[]> rows = new ArrayList<>();
        addRow(rows, "Pedido", payloadValue(payload, "orderId", ""));
        addRow(rows, "Producto", payloadValue(payload, "itemsSummary", ""));
        addRow(rows, "Total", payloadValue(payload, "totalAmount", ""));

        switch (template) {
            case "payment-received" -> {
                addRow(rows, "Pago", payloadValue(payload, "paymentId", ""));
                addRow(rows, "Monto acreditado", payloadValue(payload, "amount", ""));
                addRow(rows, "Saldo restante", payloadValue(payload, "remainingBalance", ""));
            }
            case "shipment-confirmation" -> {
                addRow(rows, "Envío", payloadValue(payload, "shipmentId", ""));
                addRow(rows, "Estado del envío", payloadValue(payload, "shipmentStatusLabel", payloadValue(payload, "shipmentStatus", "")));
            }
            case "payment-pending" -> {
                addRow(rows, "Estado", payloadValue(payload, "statusLabel", payloadValue(payload, "orderStatusLabel", "")));
                addRow(rows, "Saldo pendiente", payloadValue(payload, "remainingBalance", ""));
            }
            case "order-status-changed" -> addRow(rows, "Estado", payloadValue(payload, "statusLabel", payloadValue(payload, "orderStatusLabel", "")));
            default -> {
                addRow(rows, "Estado", payloadValue(payload, "statusLabel", payloadValue(payload, "orderStatusLabel", "")));
                addRow(rows, "Momento", payloadValue(payload, "momentLabel", ""));
            }
        }

        if (rows.isEmpty()) {
            return payloadRows(payload, "");
        }
        return rows.toArray(String[][]::new);
    }

    private void addRow(List<String[]> rows, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        rows.add(new String[]{label, value});
    }

    private String payloadValue(Map<String, Object> payload, String key, String fallback) {
        if (payload == null) {
            return fallback;
        }
        Object value = payload.get(key);
        if (value == null) {
            return fallback;
        }
        String stringValue = value.toString();
        return stringValue.isBlank() ? fallback : stringValue;
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

      private String[][] payloadRows(Map<String, Object> payload, String recipient) {
        List<String[]> rows = new ArrayList<>();
        if (recipient != null && !recipient.isBlank()) {
            rows.add(new String[]{"Destinatario", recipient});
        }

        if (payload != null && !payload.isEmpty()) {
          payload.forEach((key, value) -> rows.add(new String[]{
              prettyKey(key),
              value == null ? "—" : value.toString()
          }));
        } else {
          rows.add(new String[]{"Payload", "Sin datos adicionales"});
        }

        return rows.toArray(String[][]::new);
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

    private String marketplaceLayout(String accentColor, String body) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f3f4f6;padding:28px 0;">
                <tr>
                  <td align="center">
                    <table width="620" cellpadding="0" cellspacing="0" border="0" style="background:#ffffff;border-radius:28px;overflow:hidden;max-width:620px;width:100%%;box-shadow:0 18px 45px rgba(15,23,42,0.08);">
                      <tr>
                        <td style="background:%s;padding:20px 32px;">
                          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td>
                                <span style="font-size:18px;font-weight:800;color:#ffffff;letter-spacing:0.3px;">Broker Marketplace</span>
                              </td>
                              <td align="right">
                                <span style="font-size:12px;color:rgba(255,255,255,0.82);">Actualizaciones de tu compra</span>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      %s
                      <tr>
                        <td style="padding:22px 32px;border-top:1px solid #e5e7eb;background:#f8fafc;">
                          <p style="margin:0;font-size:11px;color:#94a3b8;text-align:center;line-height:1.7;">
                            Este es un correo automático generado por Broker Marketplace.<br/>
                            Te avisaremos cuando tu compra tenga una nueva novedad.
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

    private String prettyKey(String value) {
      if (value == null || value.isBlank()) {
        return "Campo";
      }

      String normalized = value.replace('_', ' ').replace('-', ' ');
      normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
      return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
