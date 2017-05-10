package main.scala

import java.io.{File, PrintWriter}

import modules._
import org.slf4j._
import scopt.options._

/**
 * created by aaronmck on 12/16/14
 *
 * Copyright (c) 2014, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */
object Main extends App {
  // parse the command line arguments
  val parser = new scopt.options.PeelParser[Config]("FlashFry") {
    head("FlashFry", "1.2")
    override def errorOnUnknownArgument = false

    // *********************************** Inputs *******************************************************
    opt[String]("analysis") required() valueName ("<string>") action {
      (x, c) => c.copy(analysisType = Some(x))
    } text ("The run type: one of: discovery, score, or random")

    // some general command-line setup stuff
    note("Find CRISPR targets across the specified genome\n")
    help("help") text ("prints the usage information you see here")
  }

  val logger = LoggerFactory.getLogger("Main")

  parser.peel(args, Config()) map {
    case(config, remainingArgs) => {

      val initialTime = System.nanoTime()

      config.analysisType.get match {
        case "index" => {
          (new BuildOffTargetDatabase()).runWithOptions(args)
        }
        case "discover" => {
          (new OffTargetDiscovery()).runWithOptions(args)
        }
        case "random" => {
          (new GenerateRandomFasta()).runWithOptions(args)
        }
        case "score" => {
          (new ScoreResults()).runWithOptions(args)
        }
        case "dump" => {
          (new DumpDatabase()).runWithOptions(args)
        }
        case _ => {
          throw new IllegalStateException("")
        }
      }

      logger.info("Total runtime " + "%.2f".format((System.nanoTime() - initialTime) / 1000000000.0) + " seconds")
    }
  }
}

/*
 * the configuration class, it stores the user's arguments from the command line, set defaults here
 */
case class Config(analysisType: Option[String] = None, scoringMetrics: Seq[File] = Seq())