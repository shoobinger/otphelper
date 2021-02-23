use std::str::FromStr;

use lazy_static::lazy_static;
use regex::{Regex};
use rusqlite::{Connection, Error, params};
use tiny_http::{Header, HeaderField, Request, Response, Server, StatusCode};

fn main() {
    let server = Server::http("0.0.0.0:8678").unwrap();
    let db = Connection::open("otp.sqlite").unwrap();

    db.execute(
        "CREATE TABLE IF NOT EXISTS otp (
                id          INTEGER PRIMARY KEY,
                code        TEXT NOT NULL
        )", params![],
    ).unwrap();

    db.execute(
        "CREATE TABLE IF NOT EXISTS message (
                id          INTEGER PRIMARY KEY,
                text        TEXT NOT NULL
        )", params![],
    ).unwrap();

    for mut request in server.incoming_requests() {
        log_request(&request);
        if request.url() == "/message" {
            let method = request.method().as_str();
            if method != "POST" {
                request.respond(Response::empty(405)).unwrap();
                continue;
            }

            if !contains_header(&request, "Content-Type", "text/plain") {
                request.respond(Response::empty(415)).unwrap();
                continue;
            }

            let mut body = String::new();
            request.as_reader().read_to_string(&mut body).unwrap();
            db.execute("INSERT INTO message (text) VALUES (?1)", params![body]).unwrap();
            match extract_otp(&body) {
                None => {}
                Some(c) => {
                    db.execute("INSERT INTO otp (code) VALUES (?1)", params![c]).unwrap();
                }
            }

            request.respond(Response::empty(StatusCode(200))).unwrap();
            continue;
        }

        if request.url() == "/otp" {
            let method = request.method().as_str();
            if method != "GET" {
                request.respond(Response::empty(405)).unwrap();
                continue;
            }

            if !contains_header(&request, "Accept", "text/plain") {
                request.respond(Response::empty(406)).unwrap();
                continue;
            }

            let mut stmt =
                db.prepare("SELECT id, code FROM otp ORDER BY id DESC LIMIT 1").unwrap();
            let (text, code) =
                match stmt.query_row(params![], |row| row.get(1)) {
                    Ok(otp) => (otp, 200),
                    Err(e) => match e {
                        Error::QueryReturnedNoRows => { (String::from("<No messages>"), 200) }
                        _ => (format!("{}", e), 500),
                    },
                };

            request.respond(Response::from_string(text)
                .with_status_code(code)
                .with_header(Header::from_str("Content-Type: text/plain").unwrap()))
                .unwrap();
            continue;
        }

        request.respond(Response::empty(StatusCode(404))).unwrap();
    }
}

fn contains_header(request: &Request, name: &str, value: &str) -> bool {
    request.headers().into_iter().any(|h|
        h.field == HeaderField::from_str(name).unwrap()
            && h.value == value)
}

fn extract_otp(message: &str) -> Option<&str> {
    lazy_static! {
        static ref OTP_RE: Regex = Regex::new(r"[0-9]+").unwrap();
    }
    let captures = OTP_RE.captures(message).unwrap();

    captures.get(0).map(|s| s.as_str())
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
