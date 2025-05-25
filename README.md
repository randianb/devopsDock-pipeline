# DEVOPS DOCK PROJECT

## DevOps Tooling Cluster with Docker Compose

    88PPP.   ,d8PPPP d88   88 88888888 8888PPPp, 88888888      88PPP. 88888888 doooooo 888  ,dP 
    88   8   d88ooo  d88   88 888  888 8888    8 88ooooPp      88   8 888  888 d88     888o8P'  
    88   8 ,88'      d88_o8P' 888  888 8888PPPP'        d8     88   8 888  888 d88     888 Y8L  
    88oop' 88bdPPP   Y88P'    888oo888 888P      8888888P      88oop' 888oo888 d888888 888  `8p 
                                                                                    

This repository provides a Docker Compose configuration for deploying a minimal DevOps tooling cluster, including Nexus, Jenkins, SonarQube, PostgreSQL, Traefik, pgAdmin, and GitLab. Tailored for development, testing, and hands-on exploration, it offers a streamlined environment to discover and experiment with tools for continuous integration, delivery, and monitoring.

![DevOps Banner](devops_banner.png)

## Prerequisites

- 🐧 Linux Machine or 💻 Docker Desktop for Windows Users. 
- 🐳 Docker Engine installed on your machine. 
- 🛠️ Docker Compose installed on your machine. 
- 🌐 Domain names configured for each service (e.g., `nexus-devops.com`, `jenkins-devops.com`, etc.). 
- 🔒 SSL certificates obtained for secure communication (e.g., Let's Encrypt). 
- 💡 Basic understanding of Docker Compose and networking concepts. 
- Add the following entries to your `/etc/hosts` file, if you're using Windows the path will be on  `C:\Windows\System32\drivers\etc\hosts`:
 
    ```
    127.0.0.1 nexus-devops.com
    127.0.0.1 traefik-devops.com
    127.0.0.1 jenkins-devops.com
    127.0.0.1 sonarqube-devops.com
    127.0.0.1 pgadmin-devops.com
    127.0.0.1 gitlab-devops.com
    ```

## Accessing Services

- Nexus: [http://nexus-devops.com](http://nexus-devops.com:8081)
- Jenkins: [http://jenkins-devops.com](http://jenkins-devops.com:8080)
- SonarQube: [http://sonarqube-devops.com](http://sonarqube-devops.com:9000)
- pgAdmin: [http://pgadmin-devops.com](http://pgadmin-devops.com)
- GitLab: [http://gitlab-devops.com](http://gitlab-devops.com:8929)
- Traefik Dashboard: [http://traefik-devops.com](http://traefik-devops.com:8082)

## Cluster Configuration

The Docker Compose configuration includes the following services:

- Nexus: Artifact repository manager.
- Jenkins: Automation server for continuous integration and continuous delivery.
- SonarQube: Code quality and security analysis platform.
- PostgreSQL: Relational database management system for storing application data.
- Traefik: Reverse proxy and load balancer for routing traffic.
- pgAdmin: Web-based administration tool for PostgreSQL.
- GitLab: Complete DevOps platform for version control, CI/CD, and collaboration.

## Usage

1. Clone this repository to your local machine.
2. Customize environment variables in the `.env` file according to your requirements.
3. Run `docker-compose up -d` to start the cluster in detached mode.
4. Access the services using the provided URLs.
5. Visit Traefik Dashboard at `http://traefik-devops.com:8082` for monitoring and management.

To stop the cluster, run the following command:

```bash
docker-compose down
```

## Configuration Details

Detailed configuration settings for each service are available in the `docker-compose.yml` file.

## License

This project is licensed under the [MIT License](LICENSE).

## Contributors

- [HARROU ANOUAR](https://github.com/anouarharrou)

Feel free to contribute by opening issues or pull requests.

Happy coding! 🚀
