set -e
: ${TAG:?"Need to set TAG for release"}
docker build -f Dockerfile -t crossref/event-data-reverse:$TAG .
docker push crossref/event-data-reverse:$TAG
