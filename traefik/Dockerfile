# Start from the base Traefik image
FROM traefik:v2.9

# Install OpenSSL
RUN apk --no-cache add openssl

# Create directory for SSL certificates
RUN mkdir -p /home/devops/data/certificates

# Generate SSL certificates using OpenSSL
WORKDIR /home/devops/data/certificates
RUN openssl req -x509 -newkey rsa:2048 -keyout /home/devops/data/certificates/key.key -out /home/devops/data/certificates/cer.crt -days 3560 -nodes -subj "/C=MA/ST=Casablanca/L=Casablanca/O=devops/CN=traefik-devops.com"

# Expose necessary ports
EXPOSE 80 443 8080
