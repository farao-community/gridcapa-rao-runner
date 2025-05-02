[![Actions Status](https://github.com/farao-community/gridcapa-rao-runner/workflows/CI/badge.svg)](https://github.com/farao-community/gridcapa-rao-runner/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=farao-community_gridcapa-rao-runner&metric=coverage)](https://sonarcloud.io/component_measures?id=farao-community_gridcapa-rao-runner&metric=coverage)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=farao-community_gridcapa-rao-runner&metric=alert_status)](https://sonarcloud.io/dashboard?id=farao-community_gridcapa-rao-runner)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/farao-community)

# gridcapa-rao-runner

Server based on spring-boot dedicated to launch rao computations for one timestamp

## Requirements
In order to build **gridcapa-rao-runner-app**, you need the following environment available:
  - Install JDK *(21)*
  - Install Maven latest version
  - Install Docker (optional)
  - Install Docker Compose (optional)
  
## Launch required applications
You need the following service in order to launch the application:
  - RabbitMQ server as message brocker
  - MinIO server as file storage system

You can either launch them locally on your computer or use a pre-compiled docker container.

##### Use a pre-compiled docker container:
To launch required containers (minio and rabbit-mq), on the root of this project run the following command:

```bash
docker-compose up 
```
To stop it use:

```bash
docker-compose down 
```

##### Or install and launch locally the required servers:
  - RabbitMQ server: https://www.rabbitmq.com/download.html
  - Min IO server: https://docs.min.io/docs/minio-quickstart-guide.html

