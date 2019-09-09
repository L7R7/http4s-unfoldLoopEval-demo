# http4s-unfoldLoopEval-demo

This is a small demo project demonstrating the use of `unfoldLoopEval` when implementing a streaming consumer for a paginated HTTP resource.
This combinator is introduced by [Chris Davenport](https://github.com/ChristopherDavenport) in [this PR](https://github.com/functional-streams-for-scala/fs2/pull/1594/).

In this particular case, `unfoldLoopEval` greatly simplifies code that used `unfoldEval` before. In particular, this function:

```scala
def crawl(httpApp: HttpApp[IO]): Stream[IO, Response[IO]] =
  Stream.unfoldEval[IO, Option[Request[IO]], Response[IO]](Some(createRequest("/feed/1"))) {
    case Some(req) =>
      httpApp.run(req).map(response => Some((response, requestFromNextUri(response))))
    case None      => IO(None)
  }
```
is replaced by this:

```scala
def crawl(httpApp: HttpApp[IO]): Stream[IO, Response[IO]] =
  Stream.unfoldLoopEval(createRequest("/feed/1")) { request =>
    httpApp.run(request).map(response => (response, requestFromNextUri(response)))
  }
```
Note that:
* In the former version, the tuple from `Response[IO]` and `Request[IO]` needs to be wrapped in an `Option`.
The response is never optional though, there will be a response for every request.
Also, the pattern match feels a bit weird.
In the latter version, only the request is wrapped in an `Option`.
This makes much more sense as this indicates whether or not there's a new request to continue the loop.
* `unfoldLoopEval` doesn't need type annotations to compile (one might add them for clarity, but the compiler is happy without them)

## Try it

Start the server

```shell script
sbt "runMain com.l7r7.lab.http4s.stream.paged.server.Server"
```
    
This will start up a web server with an HTTP API containing four endpoints:

    /feed/4
    /feed/3
    /feed/2
    /feed/1
    
All the pages form a circular linked list with their Response headers: `1 -> 2 -> 3 -> 4 -> 1`.
The loop from page `4` back to page `1` is done deliberately for demonstration purposes.
In real life, this link wouldn't exist. 
The content of the individual pages is a `multipart/package` structure containing several parts of information.
Right now all pages serve the same content, but this doesn't matter for this demo.

The main idea is to build a client that consumes all the parts, starting at page `1`, crawling forward using the response headers.
It will consume 50 parts, print out each part's headers along the way and then stop.

You can run the client with
```shell script
sbt "runMain com.l7r7.lab.http4s.stream.paged.ClientApp"
```

## Credits
Thanks to [Chris Davenport](https://github.com/ChristopherDavenport) for adding this nice combinator and for [tweeting about this](https://twitter.com/davenpcm/status/1170428996795781120)!