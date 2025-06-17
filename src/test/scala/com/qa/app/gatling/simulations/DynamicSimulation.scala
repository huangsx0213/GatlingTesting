package com.qa.app.gatling.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DynamicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://www.uk.com")


  val scn = scenario("Dynamic Test for UKB-TC001")
    .repeat(1) {
      exec(http("UKB-TC001")
        .post("/sit/outbound")
        .body(StringBody("""{
  \"userId\": \"usid0001\",
  \"username\": \"vhuang1\",
  \"department\": \"wsit\",
  \"active\": true,
  \"roles\": [
    \"DEV\",
    \"TEST\"  ]
}"""))
        .check(status.is(200)))
    }

  setUp(
    scn.inject(
      rampUsers(1) during (0 seconds)
    ).protocols(httpProtocol)
  )
}