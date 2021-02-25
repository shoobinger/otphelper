# OTP Helper — receive SMS OTP on your PC

Having to use a mobile phone to receive OTPs is a nuisance and not many sites adopted TOTP or WebauthN yet.

## How to use
1. Launch [server](https://github.com/suive/otphelper/blob/master/server/README.md).
2. Install [Android client app](https://github.com/suive/otphelper/blob/master/android-client/README.md), give it the permission to receive and read SMS, specify server connection details.
The client application will listen to all incoming SMS messages, extract OTP codes and send them to the server.
3. Get the last received OTP code on your PC, e.g.
   ```bash 
   curl "$SERVER_URL/otp" -H "Accept: text/plain"
   ```

## Tips
- The client app supports Basic authorization, but the server does not (use a reverse proxy).
- To type the received OTP code automatically, use `xdotool` (or `ydotool` on Wayland): `curl ... | xdotool type --file -`
