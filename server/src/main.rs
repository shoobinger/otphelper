use std::io::Error;
use std::str::FromStr;

use tiny_http::{Header, HeaderField, Request, Response, Server, StatusCode};

fn main() {
    let server = Server::http("0.0.0.0:8678").unwrap();
    println!("HTTP server is running on {}", server.server_addr());

    let mut otp = String::from("");

    for mut request in server.incoming_requests() {
        log_request(&request);

        if request.url() == "/otp" {
            if !contains_header(&request, "Accept", "text/plain") {
                request.respond(Response::empty(406)).unwrap();
                continue;
            }

            let method = request.method().as_str();
            if method == "GET" {
                return_text(request, &otp).unwrap();
                continue;
            }

            if method == "POST" {
                if !contains_header(&request, "Content-Type", "text/plain") {
                    request.respond(Response::empty(406)).unwrap();
                    continue;
                }

                request.as_reader().read_to_string(&mut otp).unwrap();
                return_text(request, &otp).unwrap();
                continue;
            }

            request.respond(Response::empty(405)).unwrap();
            continue;
        }

        request.respond(Response::empty(404)).unwrap();
    }
}

fn contains_header(request: &Request, name: &str, value: &str) -> bool {
    request.headers().into_iter().any(|h|
        h.field == HeaderField::from_str(name).unwrap()
            && h.value == value)
}

fn log_request(request: &Request) {
    println!(
        "Received request method: {:?}, url: {:?}, headers: {:?}",
        request.method(),
        request.url(),
        request.headers()
            .into_iter().map(|h| format!("{}", h))
            .collect::<Vec<String>>().join(", ")
    );
}

fn return_text(request: Request, text: &str) -> Result<(), Error> {
    request.respond(Response::from_string(text)
        .with_status_code(200)
        .with_header(Header::from_str("Content-Type: text/plain").unwrap()))
}
