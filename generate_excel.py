import sys
import os
from openpyxl import Workbook
from datetime import datetime

def generate_excel(server_name, account, mismatch_details, status):
    """ Create an Excel report and return the filename. """

    # Define Excel file name with timestamp
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    excel_file = f"OS_Image_Report_{server_name}_{timestamp}.xlsx"

    # Create a new Excel workbook and worksheet
    wb = Workbook()
    ws = wb.active
    ws.title = "OS Image Report"

    # Add headers
    headers = ["Server Name", "Account", "Mismatch Details", "Status"]
    ws.append(headers)

    # Add data
    ws.append([server_name, account, mismatch_details, status])

    # Auto-adjust column width
    for col in ws.columns:
        max_length = max(len(str(cell.value)) for cell in col) + 2
        ws.column_dimensions[col[0].column_letter].width = max_length

    # Save the file
    wb.save(excel_file)
    print(excel_file)  # Return filename

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("❌ ERROR: Missing arguments for Excel generation")
        sys.exit(1)

    server_name = sys.argv[1]
    account = sys.argv[2]
    mismatch_details = sys.argv[3]
    status = sys.argv[4]

    generate_excel(server_name, account, mismatch_details, status)
