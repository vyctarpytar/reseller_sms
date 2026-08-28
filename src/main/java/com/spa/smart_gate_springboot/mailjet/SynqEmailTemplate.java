package com.spa.smart_gate_springboot.mailjet;

import com.spa.smart_gate_springboot.utils.AppTime;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;

// Every style is inline and the layout is tables: Gmail/Outlook strip <head><style> and
// have no flexbox/grid support, so a <style> block or flex here would render unstyled.
public final class SynqEmailTemplate {

    private static final String DEFAULT_BASE_URL = "https://backend.synqafrica.co.ke";

    private static String publicBaseUrl = DEFAULT_BASE_URL;

    private static final String FONT = "Arial,Helvetica,sans-serif";

    private static final String SKELETON = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1"/>
            <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
            <title>%1$s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#fafaf9;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%;background-color:#fafaf9;padding:32px 16px;">
            <tr>
            <td align="center" style="padding:0;">
            <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%;max-width:600px;">
            <tr>
            <td align="center" style="padding:0 0 20px;">
            <img src="%2$s/api/v2/public/brand/logo.png" width="150" alt="Synq Africa" style="display:block;border:0;outline:none;text-decoration:none;width:150px;height:auto;"/>
            </td>
            </tr>
            <tr>
            <td style="background-color:#ffffff;border:1px solid #E7E2DB;border-radius:14px;overflow:hidden;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%;">
            <tr>
            <td height="3" style="height:3px;background-color:#D96C3B;line-height:3px;font-size:0;">&nbsp;</td>
            </tr>
            <tr>
            <td style="padding:28px 32px 8px;color:#69472E;font-family:%6$s;font-size:19px;font-weight:700;line-height:1.35;">%3$s</td>
            </tr>
            <tr>
            <td style="padding:0 32px 30px;color:#334155;font-family:%6$s;font-size:14px;line-height:1.65;">%4$s</td>
            </tr>
            </table>
            </td>
            </tr>
            <tr>
            <td align="center" style="padding:22px 12px 0;color:#64748b;font-family:%6$s;font-size:12px;line-height:1.6;">Value Added Mobile Solutions</td>
            </tr>
            <tr>
            <td align="center" style="padding:6px 12px 0;color:#64748b;font-family:%6$s;font-size:12px;line-height:1.6;">&copy; %5$s Synq Africa Holdings Limited. All rights reserved.</td>
            </tr>
            <tr>
            <td align="center" style="padding:6px 12px 0;color:#94a3b8;font-family:%6$s;font-size:12px;line-height:1.6;">This is an automated message from Synq Africa.</td>
            </tr>
            </table>
            </td>
            </tr>
            </table>
            </body>
            </html>
            """;

    private SynqEmailTemplate() {
    }

    public static String render(String heading, String bodyHtml) {

        String safeHeading = escape(heading == null ? "" : heading.trim());
        String body = bodyHtml == null ? "" : bodyHtml;

        return SKELETON.formatted(
                safeHeading.isBlank() ? "Synq Africa" : safeHeading,
                publicBaseUrl,
                safeHeading,
                body,
                String.valueOf(AppTime.today().getYear()),
                FONT);
    }

    public static String paragraph(String text) {

        String safe = escape(text == null ? "" : text).replace("\n", "<br/>");

        return "<p style=\"margin:0 0 14px;color:#334155;font-family:" + FONT
                + ";font-size:14px;line-height:1.65;\">" + safe + "</p>";
    }

    public static String highlight(String text) {

        String safe = escape(text == null ? "" : text).replace("\n", "<br/>");

        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="width:100%%;margin:4px 0 18px;background-color:#F2EFEA;border-left:4px solid #D96C3B;border-radius:8px;">
                <tr><td style="padding:14px 16px;color:#69472E;font-family:%1$s;font-size:14px;line-height:1.6;font-weight:600;">%2$s</td></tr>
                </table>
                """.formatted(FONT, safe);
    }

    public static String infoRow(String label, String value) {

        String safeLabel = escape(label == null ? "" : label);
        String safeValue = (value == null || value.isBlank()) ? "&#8212;" : escape(value);

        return ("<tr><td width=\"40%%\" style=\"width:40%%;padding:8px 0;border-bottom:1px solid #E7E2DB;color:#64748b;"
                + "font-family:%1$s;font-size:13px;line-height:1.5;\">%2$s</td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #E7E2DB;color:#334155;font-family:%1$s;"
                + "font-size:13px;font-weight:600;line-height:1.5;\">%3$s</td></tr>").formatted(FONT, safeLabel, safeValue);
    }

    public static String infoTable(String rowsHtml) {

        if (rowsHtml == null || rowsHtml.isBlank()) return "";

        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
                + " style=\"width:100%;margin:4px 0 18px;\">" + rowsHtml + "</table>";
    }

    public static String button(String label, String url) {

        if (url == null || url.isBlank()) return "";

        String safeLabel = escape(label == null || label.isBlank() ? "Open" : label);

        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:6px auto 18px;">
                <tr><td align="center" style="background-color:#D96C3B;border-radius:8px;">
                <a href="%2$s" target="_blank" style="display:inline-block;padding:12px 26px;color:#ffffff !important;text-decoration:none;font-weight:600;font-size:14px;font-family:%1$s;">%3$s</a>
                </td></tr>
                </table>
                """.formatted(FONT, escape(url), safeLabel);
    }

    public static String toPlainText(String html) {

        if (html == null || html.isBlank()) return "";

        String out = html
                .replaceAll("(?is)<head[^>]*>.*?</head>", "")
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)</tr\\s*>", "\n")
                .replaceAll("(?i)</t[dh]\\s*>", " ")
                .replaceAll("(?s)<[^>]+>", "");

        out = out.replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&#8212;", "-")
                .replace("&copy;", "(c)")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");

        return out.replace("\r", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public static String escape(String raw) {

        if (raw == null) return "";

        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Component
    static class EmailBrandConfig {

        @Value("${app.public-base-url:https://backend.synqafrica.co.ke}")
        private String baseUrl;

        @PostConstruct
        void apply() {
            if (baseUrl != null && !baseUrl.isBlank()) {
                publicBaseUrl = baseUrl.trim().replaceAll("/+$", "");
            }
        }
    }
}
