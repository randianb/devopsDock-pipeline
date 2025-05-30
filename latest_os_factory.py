import sys
import json
import re
import os
import subprocess
from common import logger, sgcloud
from contextlib import redirect_stdout

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))  
NOTIFY_SERVER_MISMATCH = os.path.join(SCRIPT_DIR, "../scripts/notify/notify_image_server_mismatch.sh")  
NOTIFY_REGION_MISMATCH = os.path.join(SCRIPT_DIR, "../scripts/notify/notify_image_region_mismatch.sh")
NOTIFY_DOWNGRADE = os.path.join(SCRIPT_DIR, "../scripts/notify/notify_downgrade.sh")

def print_banner():
    banner = """
       ____  _____    _________   ________________  ______  __
      / __ \/ ___/   / ____/   | / ____/_  __/ __ \/ __ \ \/ /
     / / / /\__ \   / /_  / /| |/ /     / / / / / / /_/ /\  / 
    / /_/ /___/ /  / __/ / ___ / /___  / / / /_/ / _, _/ / /  
    \____//____/  /_/   /_/  |_\____/ /_/  \____/_/ |_| /_/   

        DEVOPS SGABS - Vulnerable OS Factory Patching Script
           Secure and Maintain OS Images Across Regions  
    
                                                 ~ Anouar Harrou
    """
    print(banner)

def get_ocs_images(server_name, region):
    """Fetch and return the installed OS image for a given server in a region."""
    sg_cloud_client = sgcloud.Client(os.environ["ACCOUNT_ID"], os.environ["CLIENT_ID"], os.environ["CLIENT_SECRET"])
    try:
        with open(os.devnull, 'w') as devnull:
            with redirect_stdout(devnull):
                image_data = sg_cloud_client.get_server_image_ids(server_name, region)
                if not image_data:
                    logger.info(f"INFO: No servers found for service '{server_name}' in region '{region}'. Maybe this is the first deployment of this service in the {region} region. ")
                    return []
                else:
                    return sg_cloud_client.get_image_details(image_data, region)
    except Exception as e:
        logger.error(f"ERROR: Unable to retrieve image data for server '{server_name}' in region '{region}'. Exception: {e}")
        sys.exit(1)


def extract_version(image_name):
    """Extract numerical year and week number from an OS image name."""
    match = re.search(r'(\d{4})_.(\d{2})', image_name)
    if match:
        return int(match.group(1)), int(match.group(2))  # Return (year, week)
    logger.info(f"WARNING: Unable to extract version from {image_name}. Defaulting to (0,0).")
    return (0, 0)  # Fallback if format isn't found

def is_newer(image1, image2):
    """Compare two OS images based on year and week."""
    return extract_version(image1) > extract_version(image2)

def determine_common_latest_image(latest_paris, latest_north, list_paris, list_north, account):
    """Determine the latest common OS image between Paris and North."""
    if latest_paris == latest_north:
        return latest_paris  # Both regions have the same latest OS image
    else:
        logger.info(f"INFO: The latest image in Paris {latest_paris} is not the same as in North {latest_north}. Sending notification.")
        subprocess.run(["sh", NOTIFY_REGION_MISMATCH, latest_paris, latest_north, account], check=True)
        
    common_images = sorted(set(list_paris) & set(list_north), key=extract_version, reverse=True)
    if common_images:
        return common_images[0]  # Pick the latest common OS image

    logger.error("ERROR: No common OS image found between Paris and North.")
    sys.exit(1)

def check_deployment_needed(region, installed_image, os_factory):
    """Check if deployment is needed for a given region."""
    if installed_image == os_factory:
        logger.info(f"[{region}] Latest common OS image is already installed: {installed_image}")
        return os_factory 

    if is_newer(installed_image, os_factory):
        logger.debug(f"[{region}] Installed OS ({installed_image}) is newer than the common OS ({os_factory}). No downgrade needed.")
        return installed_image  
    if installed_image == "Null":
        logger.debug(f"[{region}] No image is installed in this region.")
    else:
        logger.debug(f"[{region}] Installed OS ({installed_image}) is outdated. Upgrade to {os_factory} is required.")
    
    return os_factory

def main():
    print_banner()

    if len(sys.argv) < 3:
        logger.info("ℹ️ Usage: python3 latest_os_factory.py <server_name> <region>")
        sys.exit(1)

    server_name = sys.argv[1]
    region_input = sys.argv[2]
    trigram = os.environ['TRIGRAM']
    irt = os.environ['IRT']
    env = os.environ['CLOUD_ENV']
    account = f"{trigram}_{irt}_{env}".upper()
    # Ensure regions input is correctly parsed
    try:
        regions = json.loads(region_input)
        if not isinstance(regions, list):
            raise ValueError("Regions input should be a list")
    except (json.JSONDecodeError, ValueError) as e:
        logger.error(f"ERROR: Invalid format for regions: {region_input} - {e}")
        sys.exit(1)

    final_image = None

    sg_cloud_client = sgcloud.Client(os.environ['ACCOUNT_ID'], os.environ['CLIENT_ID'], os.environ['CLIENT_SECRET'])

    if "paris" in regions and "north" in regions:
        logger.debug("Both regions selected: Paris and North. Fetching latest images...")
        with open(os.devnull, 'w') as devnull:
            with redirect_stdout(devnull):
                list_os_paris = sg_cloud_client.get_images(region="paris")
                list_os_north = sg_cloud_client.get_images(region="north")
                latest_os_paris = list_os_paris[0]
                latest_os_north = list_os_north[0]

        logger.debug(f"================== Account: {account} ==================")
        logger.debug(f"List of OS Images in Paris: {list_os_paris}")
        logger.debug(f"List of OS Images in North: {list_os_north}")
        logger.debug(f"Latest OS in Paris: {latest_os_paris}")
        logger.debug(f"Latest OS in North: {latest_os_north}")
        
        # Determine common OS and check if an upgrade is needed
        os_factory = determine_common_latest_image(latest_os_paris, latest_os_north, list_os_paris, list_os_north, account)
        logger.debug(f"The Latest Common OS on Both Regions: {os_factory}")

        # Fetch installed images for both regions
        ocs_instances = {}
        installed_images = {}
        for region in regions:
            ocs_instances[region] = get_ocs_images(server_name, region)
            _installed_images = [item['image_name'] for item in ocs_instances[region]]
            if (len(_installed_images) == 0):
                logger.info(f"First deployment of the service {server_name} in the {region} region.")
            installed_images[region] = _installed_images if len(_installed_images) > 0 else ["Null"]

        logger.info(f"The OS image deployed on the matching label {server_name} in the selected regions is:")
        for region in regions:
            data = ""
            for item in ocs_instances[region]:
                data += f"[SERVER]: {item['server_name']}, [IMAGE]: {item['image_name']}\n"
            if data == "": data = "No servers found!"
            logger.debug(f"=================== Region: {region} ====================")
            logger.debug(f"{data}")
            logger.debug(f"======================================================")
        
        for region in regions:
            if len(set(installed_images[region])) > 1:
                mismatch_output = f"The Script found a mismatch on the region: 🌍 {region}, for the server: 🖥️ [{server_name}]: \n"
                for item in ocs_instances[region]:
                    mismatch_output += f"[SERVER]: {item['server_name']}, [IMAGE]: {item['image_name']}\n"
                subprocess.run(["sh", NOTIFY_SERVER_MISMATCH, server_name, account, mismatch_output], check=True)
                logger.debug("The servers have mismatched OS images. Notification triggered..")
                sys.exit(1)
            installed_images[region] = next(iter(set(installed_images[region])))
            if installed_images[region] == "Null":
                logger.info(f"No image is installed in the region {region}.")
            else:
                logger.info(f"All servers in region {region} have the same OS image: {installed_images[region]}.")

        deploy_paris = check_deployment_needed("Paris", installed_images['paris'], os_factory)
        deploy_north = check_deployment_needed("North", installed_images['north'], os_factory)

        if deploy_paris != deploy_north:
            if installed_images['paris'] == installed_images['north']:
                final_image = installed_images['paris']
            else:
                old_image = f"Paris: {deploy_paris}" if is_newer(deploy_north,deploy_paris) else f"North: {deploy_north}"
                logger.error(f"This image {old_image} risks a downgrade of servers.")
                details = f"🚨 The image located on [{old_image}] risks a downgrade of servers. \n"
                details += f"Please build a newer image in the {old_image.split(':')[0]} region and re-deploy the service {server_name}."
                subprocess.run(["sh", NOTIFY_DOWNGRADE, server_name, account, details], check=True)
                logger.debug("The OS image is downgraded. Notification triggered..")
                sys.exit(1)
        else:
            final_image = deploy_paris
        


    else:
        with open(os.devnull, 'w') as devnull:
            with redirect_stdout(devnull):
                region = regions[0]
                list_os = sg_cloud_client.get_images(region=region)
                latest_os = list_os[0]
                ocs_instances = get_ocs_images(server_name, region)
                installed_images = [item['image_name'] for item in ocs_instances]
        logger.debug(f"================== Account: {account} ==================")
        logger.info(f"Single region selected: {region}. Fetching latest image...")
        logger.debug(f"List of OS Images in {region}: {list_os}")
        
        if len(installed_images) == 0:
            logger.info(f"First deployment of the service {server_name} in the {region} region.")
            installed_images = ["Null"]
        else:
            logger.info(f"Found these servers in the region {region} with the matching label {server_name}:")
            data = ""
            for item in ocs_instances:
                data += f"[SERVER]: {item['server_name']}, [IMAGE]: {item['image_name']}\n"
            logger.debug(f"================== Region: {region} ====================")
            logger.debug(f"{data}")

        unique_images = set(installed_images)

        if len(unique_images) > 1:
            mismatch_output = f"❌The Script found a mismatch on the region: 🌍 {region}, for the server: 🖥️ [{server_name}]: \n"
            mismatch_output += "\n".join(f"[SERVER]: {item['server_name']}, [IMAGE]: {item['image_name']}" for item in ocs_instances)
            subprocess.run(["sh", NOTIFY_SERVER_MISMATCH, server_name, account, mismatch_output], check=True)
            logger.info("The servers have mismatched OS images. Notification triggered.")
            sys.exit(1)

        installed_image = next(iter(unique_images))
        if installed_image != "Null": 
            logger.info(f"All servers have the same OS image: {installed_image}")
        
        final_image = check_deployment_needed(region, installed_image, latest_os)

    logger.info(f"Final decision: {final_image}")
    return final_image

if __name__ == "__main__":
    final_result = main()
    if final_result:
        print(final_result)
