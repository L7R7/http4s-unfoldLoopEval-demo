package com.l7r7.lab.http4s.stream.paged

import cats.effect.{ IO, _ }
import com.l7r7.lab.http4s.stream.paged.ExperimentalStream.unfoldLoopEval
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.blaze._
import org.http4s.headers._
import org.http4s.multipart.{ Boundary, MultipartParser }
import org.http4s.{ HttpApp, Method, Request, Response, Uri }

import scala.concurrent.ExecutionContext

object ClientApp extends IOApp {
  def createRequest(relativeUrl: String): Request[IO] =
    Request[IO](Method.GET, Uri.unsafeFromString(s"http://localhost:8080$relativeUrl"))

  def findNextUri(response: Response[IO]): Option[Uri] = response.headers
    .filter(_.is(Link))
    .find(h => h.parsed.asInstanceOf[Link].rel.contains("next"))
    .map(h => h.parsed.asInstanceOf[Link].uri)

  def requestFromNextUri(response: Response[IO]): Option[Request[IO]] =
    findNextUri(response).map(uri => createRequest(uri.toString()))

def crawl(httpApp: HttpApp[IO]): Stream[IO, Response[IO]] =
  unfoldLoopEval(createRequest("/feed/1")) { request =>
    httpApp.run(request).map(response => (response, requestFromNextUri(response)))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client: Client[IO] =>
      crawl(client.toHttpApp)
        .flatMap(_.body.unchunk.through(MultipartParser.parseToPartsStream[IO](Boundary("gc0p4Jq0M2Yt08jU534c0p"))))
        .zipWithIndex
        .evalMap { case (s, l) =>
          IO.pure(println(s"$l\t$s")).map(_ => l)
        }
        .take(50)
        .compile.last
        .map(_.getOrElse(-1))
        .flatMap { i =>
          IO.pure(println(s"$i entries")).map(_ => ExitCode(0))
        }
    }
  }
}
