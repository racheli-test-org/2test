use iron::{AfterMiddleware, Handler, IronResult, Request, Response, status};
use iron::headers::ContentType;
use hyper::headers;
use log::trace;
use reqwest::test;
use traitobject::test;