import os
import re
import sys
import tempfile
import time
from typing import Dict, List

# noinspection PyUnresolvedReferences
from sg_iamaas import create_token, IAMAASException

from common import logger, io, util, http_client

OCS_CERTIFICATES_ENDPOINT = 'https://certificate.cloud.socgen/v2/certificates'
OCS_IMAGE_SOURCES_ENDPOINT = 'https://osfapi.eu-fr-paris.cloud.socgen/v0/image_sources'
OCS_IMAGES_ENDPOINT = 'https://osfapi.eu-fr-#region#.cloud.socgen/v0/images'
OCS_VOLUMES_ENDPOINT = "https://osblk.eu-fr-paris.cloud.socgen/v1/volumes"
OCS_CSR_ENDPOINT = "https://certificate.cloud.socgen/v2/orders/csr"
OCS_CSR_CHECK_ENDPOINT = "https://certificate.cloud.socgen/v2/csr/verification"
OCS_SERVERS_ENDPOINT = "https://ocs.eu-fr-paris.cloud.socgen/v0/servers"
OCS_SERVERS_ENDPOINT_DETAILS = "https://ocs.eu-fr-#region#.cloud.socgen/v0/servers/detail"
OCS_SERVER_IMAGE_ENDPOINT= "https://ocs.eu-fr-#region#.cloud.socgen/v0/images"
OCS_PORTS_ENDPOINT = "https://ocs.eu-fr-paris.cloud.socgen/v0/ports"
OCS_ORDER_REVOKE_ENDPOINT = "https://certificate.cloud.socgen/v2/orders/revocation"
DEFAULT_CERTIFICATES_EMAIL = "SGABS-DF-DEVOPS@bhfm-ma.fr.socgen.com"
MAX_RETRIES = 3
CA = io.root_ca()


class Client:

    def __init__(self, account_id, client_id, client_secret):
        self.client_id = client_id
        self.client_secret = client_secret
        self.account_id = account_id

    @staticmethod
    def create(credentials: dict):
        creds = credentials
        if 'sgcloud' in creds:
            creds = creds['sgcloud']
        client = Client(creds['client_id'],
                        creds['client_secret'], creds['account_id'])
        client.set_env()
        logger.info(f"Credentials loaded: account_id={client.account_id}")
        return client

    @staticmethod
    def load_env(credentials: Dict):
        creds = credentials
        if 'sgcloud' in creds:
            creds = creds['sgcloud']
        os.environ['ACCOUNT_ID'] = creds['account_id']
        os.environ['CLIENT_ID'] = creds['client_id']
        os.environ['CLIENT_SECRET'] = creds['client_secret']

    def set_env(self):
        os.environ['ACCOUNT_ID'] = self.account_id
        os.environ['CLIENT_ID'] = self.client_id
        os.environ['CLIENT_SECRET'] = self.client_secret

    def get_iam_token(self, scopes: List[str]) -> str:
        try:
            token_dict = create_token(
                self.client_id,
                self.client_secret,
                self.account_id,
                scopes
            )
        except IAMAASException:
            raise Exception(
                f"Could not retrieve the IAM token, please check your credentials and if your account has the right "
                f"for the following scopes {self.account_id}/{scopes} "
            )
        return token_dict["access_token"]

    @staticmethod
    def revoke_certificates(cn, access_token: str, skip_id: str):
        logger.debug(f"Invalidating certificates that are not {skip_id}")

        res = http_client.get(
            OCS_CERTIFICATES_ENDPOINT,
            params={
                "subjectDn": f"contains|{cn}",
                "status": "GENERATED"
            },
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )
        if res.status_code == 200:
            for cert in res.json()['certificates']:
                if cert['certificateId'] != skip_id:
                    revok = http_client.post(
                        OCS_ORDER_REVOKE_ENDPOINT,
                        headers={
                            "Content-Type": "application/json",
                            "Authorization": f"Bearer {access_token}"
                        },
                        data=util.to_json({
                            "certificateId": cert['certificateId'],
                            "revocationReason": "cessationOfOperation"
                        })
                    )
                    logger.debug(
                        f"Revocation submitted for certificate {cert['certificateId']} ---> {revok.status_code}")

    def get_certificate(self, cn, csr_content: str, pkey: str) -> str:

        pkey_modulus = util.check_output(
            f"openssl rsa -noout -modulus -in {pkey} | openssl md5")

        logger.debug(f"getting cert for key: {pkey_modulus}")

        csr_content = re.sub(r'\n', '\\n', csr_content.strip())
        access_token = self.get_iam_token(
            ["pkiaas:readwrite", "pkiaas:delete"])
        res = http_client.post(
            OCS_CSR_CHECK_ENDPOINT,
            data=util.to_json({"csr": csr_content}),
            headers={"Content-Type": "application/json"}
        )

        logger.debug(f"Checking if csr is valid --> {res.status_code}")

        res = http_client.post(
            OCS_CSR_ENDPOINT,
            data=util.to_json({
                "caShortName": "unipass-server",
                "csr": csr_content,
                # "tags": tags,
                "notifications": {
                    "email": DEFAULT_CERTIFICATES_EMAIL
                }
            }),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        logger.debug(f"CSR creation request {cn} --> {res.status_code} ")

        if res.status_code == 409:
            logger.debug(f"csr already exists {cn}")
            logger.debug(res.text)
            res = http_client.get(
                OCS_CERTIFICATES_ENDPOINT,
                params={
                    "subjectDn": f"contains|{cn}",
                    "status": "GENERATED",
                    "orderBy": "+notBefore",
                },
                headers={
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {access_token}"
                }
            )

            data = res.json()
            logger.debug(util.to_json(data, 4))

            for item in data["certificates"]:
                cert = item["certificate"]["data"]
                cert_file = os.path.join(tempfile.gettempdir(), "cert.pem")
                io.write_to_file(cert_file, cert.strip())
                cert_modulus = util.check_output(
                    f"openssl x509 -noout -modulus -in {cert_file} | openssl md5")
                logger.debug(f"compare with {cert_modulus}")
                if cert_modulus == pkey_modulus:
                    return cert

            raise Exception(
                "No matching certificate found for this private key")

        logger.debug(f"CSR created for {cn}")

        certificate_id = res.json()["certificateId"]
        self.revoke_certificates(cn, access_token, certificate_id)

        # Wait for certificate to be generated
        for i in range(10):
            time.sleep(2)

            logger.debug(f"Getting certificate by id: {certificate_id}")
            res = http_client.get(
                f"{OCS_CERTIFICATES_ENDPOINT}/{certificate_id}",
                headers={
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {access_token}"
                }
            )

            if res.status_code < 400:
                #
                payload = res.json()
                if payload["certificate"]["status"] == "GENERATED":
                    return payload["certificate"]["data"]
            else:
                logger.warn(res.text)

        raise Exception("Failed to retrieve certificate")

    def get_image_sources(self):
        access_token = self.get_iam_token(["osf:read"])
        res = http_client.get(
            OCS_IMAGE_SOURCES_ENDPOINT,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        images = []
        OCS_CENTOS_VERSION = os.getenv('TARGET_OS_VERSION')
        data = res.json()['images']
        # util.pretty_print(data)

        if OCS_CENTOS_VERSION == None:
            # if OS_CENTOS_VERSION is not provided ensure that the default version selected is 7
            OCS_CENTOS_VERSION = "7"
        for image in data:
            if image["osVersion"] == "7" and OCS_CENTOS_VERSION == "7":
                # Select only the images with the version 7
                images.append(image['name'])
            if image["osVersion"] == "9" and OCS_CENTOS_VERSION == "9":
                # there is an issue not yet detected when the image with version 9 of centos is begin created
                images.append(image['name'])

        logger.info(f"get_image_sources {images}")
        return images

    def get_latest_image_source(self):
        images = self.get_image_sources()
        logger.info(f"et_latest_image_source {images}")
        return images[0]

    def get_images(self, retry=3, region="paris"):
        access_token = self.get_iam_token(["osf:read"])
        res = http_client.get(
            OCS_IMAGES_ENDPOINT.replace("#region#", region),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        images = []
        data = res.json()

        if 'images' not in data and retry > 0:
            time.sleep(2)
            return self.get_images(retry=retry - 1)

        if 'images' not in data:
            logger.error(res.text)
            raise Exception("Failed to retrieve images")

        data = data['images']

        for image in data:
            # select only RHEL 9
            if image['status'].lower() == 'created' and "rhel_9" in image['name'].lower():
                images.append(image['name'])
            else:
                logger.debug("Skipping image %s" % image['status'])
        return images

    def get_latest_image(self, region):
        logger.debug(f"Getting the latest image for {region}")
        images = self.get_images(region=region)

        if not images:
            logger.error(f"No images found for {region}")
            return None

        latest_image = images[0]  
        print(f"Latest image in {region}: {latest_image}")
        return latest_image

    def stat_servers(self):
        access_token = self.get_iam_token(["ccs:read:all"])
        res = http_client.get(
            OCS_SERVERS_ENDPOINT,
            params={"limit": 1000},
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        data = res.json()['servers']
        stats = {
            'sgcloud.servers.total': len(data),
        }
        logger.debug("Debug server structure")
        return stats

    def stat_volumes(self):
        access_token = self.get_iam_token(["osblk:read:all"])
        res = http_client.get(
            OCS_VOLUMES_ENDPOINT,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        volumes = res.json()['volumes']
        volumes_in_use = list(
            filter(lambda x: x['status'] == 'in-use', volumes))
        volumes_not_in_use = list(
            filter(lambda x: x['status'] != 'in-use', volumes))
        volumes_sizes = 0
        for v in volumes:
            volumes_sizes += v['size']
        stats = {
            'sgcloud.volumes.total': len(volumes),
            'sgcloud.volumes.in_use': len(volumes_in_use),
            'sgcloud.volumes.not_in_use': len(volumes_not_in_use),
            'sgcloud.volumes.size': volumes_sizes,
        }
        return stats

    def stat_ports(self):
        access_token = self.get_iam_token(["ccs:read:all"])
        res = http_client.get(
            OCS_PORTS_ENDPOINT,
            params={"limit": 1000},
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )

        data = res.json()['ports']
        active_ports = list(filter(lambda x: x['status'] == 'ACTIVE', data))
        inactive_active_ports = list(
            filter(lambda x: x['status'] == 'DOWN', data))
        stats = {
            'sgcloud.ports.total': len(data),
            'sgcloud.ports.active': len(active_ports),
            'sgcloud.ports.inactive': len(inactive_active_ports),
        }
        return stats

    def get_server_image_ids(self, server_name, region, retry=3):
        access_token = self.get_iam_token(["ccs:read"])

        endpoint = OCS_SERVERS_ENDPOINT_DETAILS.replace("#region#", region)
        url = f"{endpoint}?name={server_name}&status=ACTIVE"

        res = http_client.get(
            url,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {access_token}"
            }
        )
        
        if res.status_code != 200 and retry > 0:
            logger.warning(f"Retrying API request, attempts left: {retry}")
            time.sleep(2)
            return self.get_server_image_ids(server_name, region, retry - 1)
        data = res.json()
        if res.status_code != 200 or "servers" not in data:
            logger.error(f"Failed to retrieve server details: {res.text}")
            raise Exception("Failed to retrieve server details")

        image_data = []  # This will be a list of { server_name: <value>, image_id: <value> }
        for server in data["servers"]:
            if "image" in server:
                image_data.append({"server_name": server["name"], "image_id": server["image"]["id"]})
            else:
                logger.error(f"Image ID not found for server: {server['name']}")

        return image_data


    def get_image_details(self, image_data, region, retry=3):
        access_token = self.get_iam_token(["ccs:read"])
        
        servers_images = [] # Mapping: server_name -> image_name

        for entry in image_data:
            endpoint = OCS_SERVER_IMAGE_ENDPOINT.replace("#region#", region)
            url = f"{endpoint}/{entry['image_id']}"

            res = http_client.get(
                url,
                headers={
                    "accept": "application/json",
                    "Authorization": f"Bearer {access_token}"
                }
            )
            if res.status_code != 200:
                if retry > 0:
                    time.sleep(2)
                    servers_images.extend(self.get_image_details([entry], region, retry - 1))
                else:
                    logger.error(f"Failed to retrieve image details: {res.text}")
                continue

            data = res.json()
            if "image" not in data:
                logger.error("No image details found")
                continue

            image_name = data["image"]["name"]
            servers_images.append({"server_name": entry['server_name'], "image_name": image_name})

        return servers_images
        
