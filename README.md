# Event Data Reverse

Service to transform URLs back into DOIs. This service is passive and almost-stateless.

## Usage

Provided as a Docker image with Docker Compose file for testing.

To run a demo:

    docker-compose -f docker-compose.yml run -w /usr/src/app -p "8004:8004" test lein run

To run tests

    docker-compose -f docker-compose.yml run -w /usr/src/app test lein test

## Config

| Environment variable | Description                         | Default | Required? |
|----------------------|-------------------------------------|---------|-----------|
| `PORT`               | Port to listen on                   | 9990    | Yes         |
| `STATUS_SERVICE`     | Public URL of the Status service    |         | No, ignored if not supplied |
| `JWT_SECRETS`        | Comma-separated list of JTW Secrets |         | Yes |

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
