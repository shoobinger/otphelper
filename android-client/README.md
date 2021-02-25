# OTP Helper Android client

## Usage
1. Install the application.
2. Give the app an explicit permission to receive and read SMS.
3. Specify server URL and other connection details.

## Notes
Given an incoming SMS message, the app will try to extract (using a simple `[0-9]+` regex) an OTP code from it, and then send it to the server. It will retry (with an exponential backoff) if a client error or a 5xx server error occurs. If there is no internet connection available at the moment the code is received, the app will wait and the code will be sent as soon as the connection appears.
