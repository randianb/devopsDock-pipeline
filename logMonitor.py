import time
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import logging
import traceback

# Configuration
LOG_FILE_PATH = "/var/log/application.log"  # Path to monitor
EMAIL_SENDER = "your_email@example.com"  # Replace with your sender email
EMAIL_RECEIVER = "receiver_email@example.com"  # Replace with receiver email
SMTP_SERVER = "smtp.example.com"  # Replace with your SMTP server
SMTP_PORT = 587  # SMTP port
SMTP_USERNAME = "your_email@example.com"  # Replace with your SMTP username
SMTP_PASSWORD = "your_email_password"  # Replace with your SMTP password

# Logging configuration
LOGGING_FILE = "log_monitor_debug.log"  # Log file for the script's debug logs
logging.basicConfig(
    filename=LOGGING_FILE,
    level=logging.DEBUG,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def send_email_alert(log_line):
    """Send an email alert when an error is detected."""
    logging.info("Preparing to send email alert.")
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
        logging.info(f"Email alert sent to {EMAIL_RECEIVER}")
    except Exception as e:
        logging.error(f"Failed to send email alert: {e}")
        print(f"Error: Failed to send email alert. Check logs for details.")

def monitor_log_file(file_path):
    """Monitor the log file in real time."""
    logging.info(f"Starting to monitor the log file: {file_path}")
    print(f"Monitoring log file: {file_path}")
    
    try:
        # Open the file in read mode
        with open(file_path, "r") as file:
            # Move to the end of the file
            file.seek(0, 2)
            logging.debug("Moved to the end of the log file.")

            while True:
                line = file.readline()
                if not line:
                    # Wait for new entries
                    time.sleep(0.1)
                    continue
                
                logging.debug(f"Read line: {line.strip()}")
                
                # Check if "ERROR" is in the line (case-insensitive)
                if re.search(r"ERROR", line, re.IGNORECASE):
                    print(f"Error detected: {line.strip()}")
                    logging.warning(f"Error detected: {line.strip()}")
                    send_email_alert(line.strip())
    except FileNotFoundError:
        logging.error(f"Log file not found: {file_path}")
        print(f"Error: Log file not found at {file_path}")
    except Exception as e:
        logging.error(f"An unexpected error occurred: {e}")
        print(f"Error: An unexpected error occurred. Check logs for details.")

# Run the monitoring function
if __name__ == "__main__":
    try:
        monitor_log_file(LOG_FILE_PATH)
    except KeyboardInterrupt:
        logging.info("Log monitoring stopped by user.")
        print("\nStopping log file monitoring.")




####
ERROR 
2024-12-13 10:55:32,184 - INFO - Preparing to send email alert.
2024-12-13 10:55:32,234 - ERROR - Failed to send email alert: STARTTLS extension not supported by server.
2024-12-13 10:55:32,234 - DEBUG - Traceback:
Traceback (most recent call last):
  File "/home/cloud-user/logMonitor.py", line 40, in send_email_alert
    server.starttls()
  File "/usr/lib64/python3.9/smtplib.py", line 771, in starttls
    raise SMTPNotSupportedError(
smtplib.SMTPNotSupportedError: STARTTLS extension not supported by server.
