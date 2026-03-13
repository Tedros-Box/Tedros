FROM eclipse-temurin:17-jre-jammy

# ENV configuration
ENV TOMEE_VERSION=9.1.3
ENV TOMEE_FLAVOR=plume
ENV TOMEE_HOME=/usr/local/tomee

# Install required packages
RUN apt-get update && \
    apt-get install -y curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Download and extract TomEE
RUN mkdir -p ${TOMEE_HOME} \
    && curl -fSL https://archive.apache.org/dist/tomee/tomee-${TOMEE_VERSION}/apache-tomee-${TOMEE_VERSION}-${TOMEE_FLAVOR}.zip -o tomee.zip \
    && unzip tomee.zip -d /usr/local \
    && mv /usr/local/apache-tomee-${TOMEE_FLAVOR}-${TOMEE_VERSION}/* ${TOMEE_HOME}/ \
    && rm tomee.zip \
    && rm -rf /usr/local/apache-tomee-${TOMEE_FLAVOR}-${TOMEE_VERSION}

WORKDIR ${TOMEE_HOME}

# Copy customized configuration files
COPY conf/* ${TOMEE_HOME}/conf/

# Set execution permission to catalina.sh
RUN chmod +x ${TOMEE_HOME}/bin/catalina.sh

# Expose ports
EXPOSE 8080 8081 8443 8009 8005

CMD ["bin/catalina.sh", "run"]
