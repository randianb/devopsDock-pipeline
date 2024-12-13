import time
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import logging
import traceback

# Configuration
LOG_FILE_PATH = "/home/app/logs/service.log"
EMAIL_SENDER = "no-reply.monitor@domain.com"
EMAIL_RECEIVER = "anouar.harrou@domain.com"
SMTP_SERVER = "smtp-server-host"
SMTP_PORT = 25 (No Auth) , 587 with Auth

# Logging configuration
LOGGING_FILE = "debug.log"  # Log file for the script's debug logs
logging.basicConfig(
    filename=LOGGING_FILE,
    level=logging.DEBUG,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def send_email_alert(log_line):
    """Send an email alert when an error is detected."""
    try:
        msg = MIMEMultipart()
        msg['From'] = EMAIL_SENDER
        msg['To'] = EMAIL_RECEIVER
        msg['Subject'] = "ALERT: Error Detected in Log File"
        body = f"The following error was detected in the log file:\n\n{log_line}"
        msg.attach(MIMEText(body, 'plain'))

        # Connect without encryption for port 25
        with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
            # server.starttls()
            # server.login(SMTP_USERNAME, SMTP_PASSWORD)  ##Auth = false
            server.send_message(msg)
        logging.info(f"Email alert sent to {EMAIL_RECEIVER}")
        print(f"\nEmail alert sent to: {EMAIL_RECEIVER}")
    except Exception as e:
        logging.error(f"Failed to send email alert: {e}")
        logging.debug(f"Traceback:\n{traceback.format_exc()}")
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
