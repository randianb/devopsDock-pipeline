import time
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# Configuration
LOG_FILE_PATH = "/var/log/application.log"
EMAIL_SENDER = "your_email@example.com"
EMAIL_RECEIVER = "receiver_email@example.com"
SMTP_SERVER = "smtp.example.com"
SMTP_PORT = 587
SMTP_USERNAME = "your_email@example.com"
SMTP_PASSWORD = "your_email_password"

def send_email_alert(log_line):
    """Mock function to send an email alert."""
    try:
        # Create the email message
        msg = MIMEMultipart()
        msg['From'] = EMAIL_SENDER
        msg['To'] = EMAIL_RECEIVER
        msg['Subject'] = "ALERT: Error Detected in Log File"
        body = f"The following error was detected in the log file:\n\n{log_line}"
        msg.attach(MIMEText(body, 'plain'))
        
        # Send the email
        with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
            server.starttls()
            server.login(SMTP_USERNAME, SMTP_PASSWORD)
            server.send_message(msg)
        print(f"Email alert sent to {EMAIL_RECEIVER}")
    except Exception as e:
        print(f"Failed to send email alert: {e}")

def monitor_log_file(file_path):
    """Monitor the log file in real time."""
    print(f"Monitoring log file: {file_path}")
    
    # Open the file in read mode
    with open(file_path, "r") as file:
        # Move to the end of the file
        file.seek(0, 2)
        
        while True:
            line = file.readline()
            if not line:
                # Wait for new entries
                time.sleep(0.1)
                continue
            
            # Check if "ERROR" is in the line (case-insensitive)
            if re.search(r"ERROR", line, re.IGNORECASE):
                print(f"Error detected: {line.strip()}")
                send_email_alert(line.strip())

# Run the monitoring function
if __name__ == "__main__":
    try:
        monitor_log_file(LOG_FILE_PATH)
    except KeyboardInterrupt:
        print("\nStopping log file monitoring.")