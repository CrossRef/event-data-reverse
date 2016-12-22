# Event Data Reverse

FROM clojure:lein-2.7.0-alpine

MAINTAINER Joe Wass jwass@crossref.org

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN lein deps && lein compile
