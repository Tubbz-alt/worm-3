FROM openjdk:8-jre-alpine3.9

LABEL repository="https://github.com/akadir/worm" maintainer="https://github.com/akadir"

WORKDIR /worm

COPY ["./build/libs", ".docker/entrypoint.sh", "./"]

RUN chmod +x entrypoint.sh

ENV USER_TIMEZONE=UTC

ENTRYPOINT ["sh", "/worm/entrypoint.sh"]
