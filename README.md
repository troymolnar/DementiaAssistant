# Dementia Assistant

This is a small PoC application meant to run on Android Things using a Pico i.Mx7d board. It uses Google Cloud Voice Recognition, api.AI, and Firebase webhook functions. Using these services we've built a simple example of custom voice communication as a stand alone Android Things application.

This application supports a few queries such as "Where are my pills", "When should I take my medication", "I can't find the bathroom", "Where is my bedroom", "Where is the kitchen"

## Getting Started

Dementia Assistant is connected to our configured instance of api.AI and Firebase, but will need to be provided with a configuration for your own Google Cloud as this is a paid service. If you have Google Cloud set up, you can add a config file referencing it as R.raw.credential.json as follows:

{
  "type": "service_account",
  "project_id": "project-id",
  "private_key_id": "11111111111111",
  "private_key": "-----BEGIN PRIVATE KEY-----\n11111111111111==\n-----END PRIVATE KEY-----\n",
  "client_email": "project-id@project-id.iam.gserviceaccount.com",
  "client_id": "11111111111",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://accounts.google.com/o/oauth2/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/project-id%40project-id.iam.gserviceaccount.com"
}

Though, you should not store your credentials in the final version of your application.

The project requires the proper set up of an Android Things powered Pico i.Mx7d board with I/O set up as specified in PicoPorts.java.