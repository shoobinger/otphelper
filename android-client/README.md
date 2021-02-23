# Android-to-PC text message relay 
Access SMS OTP codes from your PC.

## Usage
A TCP server will be started on your Android device.
A client connecting to this server will receive the last text message from your phone inbox.

### Examples using netcat
`nc -d <IP of the device> <port>`

`nc -d 192.168.1.123 9889 | xargs -I {} notify-send -t 5000 -u normal "Last message" "{}"`  
`nc -d 192.168.1.123 9889 | xclip -selection clipboard`  
`nc -d 192.168.1.123 9889 | grep -o -E '[0-9]+' | head -1 | xdotool type --file -`  
