# OTP Helper server

This is a simple HTTP server that can store the last received OTP.

## Running

`otphelper HOST:PORT`

## Building

Build with statically-linked musl:  
`cargo build --release --target x86_64-unknown-linux-musl`

## API reference

### Get OTP

```
GET /otp
Accept: text/plain
```

### Save OTP

```
POST /otp
Content-Type: text/plain
Accept: text/plain
```
