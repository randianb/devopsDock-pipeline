#!/bin/bash

# SMTP Configuration
SMTP_HOST="smtp-goss.int.world.socgen"
SMTP_PORT=25
MAIL_FROM="no-reply.devops-monitoring@socgen.com"
MAIL_TO="hamza.ezzahi@socgen.com"

# Read server details
server_name="$1"
account="$2"
details="$3"

# Function to send an email notification
send_email() {
    local subject="🚨 Critical Alert: Image Downgrade for the servers ${server_name} (Account: ${account})"
    local date_now=$(date '+%d/%m/%Y à %H:%M')

    local message='<!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>[DEVOPS] [SGABS] : Mismatch Alerts ** Sous Surveillance **</title>
        <style>
            body {background-color: #fcfeff; font-family: Calibri, Arial, sans-serif; font-size: 14px; line-height: 1.2; margin: 0; padding: 0; min-width: 100px;}
            .container {width: 580px; max-width: 580px; margin: 0 auto; background-color: #ffffff; border-radius: 4px; border: 1px solid #e68e0b;}
            .header {background-color: #e68e0b; color: #ffffff; text-align: center; font-size: 24px; border-radius: 4px 4px 0 0; padding: 10px;}
            .title {text-align: center; font-size: 20px; font-weight: bold; margin: 15px 0;}
            .sub-header {padding: 10px; font-size: 16px; font-weight: bold;}
            .info-table {width: 100%; font-size: 13px; margin: 10px 0;}
            .info-table td {padding: 5px 15px;}
            .summary {background-color: #e68e0b; color: #ffffff; font-size: 20px; padding: 10px;}
            .description, .solution {padding: 10px 10px; font-size: 12px;}
            .footer {font-size: 12px; text-align: center; padding: 10px; color: #555;}
        </style>
    </head>
    <body>
        <table width="45%" align="center">
            <tr>
                <td align="center">
                    <div class="container">
                        <div class="header">🚨  Vulnerable OS Factory Patching Script</div>
                        <div class="title">[DEVOPS][SGABS][OS FACTORY]</div>
                        <div class="title">Critical Alert: 🚨 Image Downgrade Detected 🚨</div>
                        <div class="sub-header">DEVOPS Vulnerable Control</div>
                        <table class="info-table">
                            <tr>
                                <td><strong>📅 Date</strong></td>
                                <td>'"${date_now}"' GMT+1</td>
                            </tr>
                            <tr>
                                <td>👤 Account:</td>
                                <td>'"${account}"'</td>
                            </tr>
                            <tr>
                                <td>🖥️ Server:</td>
                                <td>'"${server_name}"'</td>
                            </tr>
                        </table>
                        <div class="summary">⚠️ Downgrade Details:</div>
                        <pre style="font-family: Calibri, Arial, sans-serif; background:#f8f9fa; padding:10px; border-left:2px border-right:2 pxsolid #d9534f; font-size:12px;">'"${details}"'</pre>
                        <div class="solution">
                            <strong>Solution:</strong>
                            <p>🔍 Please review the details and take the necessary actions.</p>
                            <p>📧 This is an automated notification from the DevOps monitoring system.</p>
                        </div>
                        <div class="footer">Devops Team</div>
                        <div class="footer">SG ABS Boulevard Sidi Mohamed Ben Abdellah, Tour Ivoire 2, Marina Casablanca Tél : +212 (0) 5 22 02 57 57 Mail : list.sgabs-df-devops@socgen.com</div>
                        <div class="footer">© 2025 - Tous droits réservés.</div>
                    </div>
                </td>
            </tr>
        </table>
    </body>
    </html>'

    local temp_file=$(mktemp)

    echo -e "Subject: ${subject}\nContent-Type: text/html; charset=UTF-8\n\n${message}" > "${temp_file}"

    curl -s --url "smtp://${SMTP_HOST}:${SMTP_PORT}" --mail-from "${MAIL_FROM}" --mail-rcpt "${MAIL_TO}" --upload-file "${temp_file}"

    rm -f "${temp_file}"
}

# Execute the email function
send_email
