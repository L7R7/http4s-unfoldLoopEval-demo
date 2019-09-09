package com.l7r7.lab.http4s.stream.paged.server

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.{ HttpRoutes, MediaType, Request, Response, Uri }

object Server extends IOApp {

  private val contentType = `Content-Type`(MediaType.multipartType("package", Some("gc0p4Jq0M2Yt08jU534c0p")))

  val routes: Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes.of[IO] {
      case _ -> Root / "feed" / "4" =>
        Ok(Contents.single(4), contentType)
          .map(_.putHeaders(
            Link(Uri.unsafeFromString("/feed/3"), Some("prev")),
            Link(Uri.unsafeFromString("/feed/4"), Some("self")),
            Link(Uri.unsafeFromString("/feed/1"), Some("next"))
          ))

      case _ -> Root / "feed" / "3" =>
        Ok(Contents.single(3), contentType)
          .map(_.putHeaders(
            Link(Uri.unsafeFromString("/feed/2"), Some("prev")),
            Link(Uri.unsafeFromString("/feed/3"), Some("self")),
            Link(Uri.unsafeFromString("/feed/4"), Some("next"))
          ))

      case _ -> Root / "feed" / "2" =>
        Ok(Contents.single(2), contentType)
          .map(_.putHeaders(
            Link(Uri.unsafeFromString("/feed/1"), Some("prev")),
            Link(Uri.unsafeFromString("/feed/2"), Some("self")),
            Link(Uri.unsafeFromString("/feed/3"), Some("next"))))

      case _ -> Root / "feed" / "1" =>
        Ok(Contents.single(1), contentType)
          .map(_.putHeaders(
            Link(Uri.unsafeFromString("/feed/1"), Some("self")),
            Link(Uri.unsafeFromString("/feed/2"), Some("next"))))
    }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(routes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}