FROM node:20-bullseye

RUN apt-get update -y
RUN apt-get install -y curl apt-transport-https ca-certificates gnupg
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" \
      | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
RUN curl https://packages.cloud.google.com/apt/doc/apt-key.gpg \
      | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
RUN apt-get update -y && \
    apt-get install -y google-cloud-cli && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install ajv and ajv-formats locally in the working directory
RUN npm install ajv ajv-formats

COPY ./local/upload-events.sh ./upload-events.sh
COPY ./local/check-gcloud-auth.sh ./local/check-gcloud-auth.sh
COPY schema/events-schema.v1.json ./events-schema.v1.json
COPY ./src/main/resources/events.json ./events.json

RUN chmod +x upload-events.sh
RUN chmod +x local/check-gcloud-auth.sh

CMD ["./upload-events.sh"]
