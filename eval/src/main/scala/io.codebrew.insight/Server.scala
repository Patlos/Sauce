package io.codebrew
package eval

import api.eval._

import com.twitter._
import finagle._
import thrift.ThriftServerFramedCodec
import builder.ServerBuilder
import builder.Server

import org.apache.thrift.protocol.TBinaryProtocol
import java.net.InetSocketAddress
import java.io.{File, FileOutputStream}
import java.lang.management.ManagementFactory

import scala.util.Try

object EvalServer {
	var server = Option.empty[Server]
	def start(evalPort: Int) {
		server = Some(
			ServerBuilder()
				.codec(ThriftServerFramedCodec())
				.name("scala-eval")
				.bindTo(new InetSocketAddress(evalPort))
				.build(new Eval.FinagledService(
					new EvalImpl, 
					new TBinaryProtocol.Factory()
				))
		)
	}

	def stop(){
		server.map(_.close())
	}
}


object Main extends App {
	// createRunningPid()

	val evalPort = (for {
		env <- Option(System.getProperty("io.codebrew.scalaEvalPort"))
		port <- Try(env.toInt).toOption
	} yield (port)).getOrElse(Config.port)

	EvalSecurity.start
	EvalServer.start(evalPort)
	Register.ready(evalPort)

	private def createRunningPid() = {
		for {
			pid <- ManagementFactory.getRuntimeMXBean.getName.split('@').headOption
			userDir <- Option(System.getProperty("user.dir")) } {

			val pidFile = new File(userDir, "RUNNING_PID")
			println(userDir)
			if (pidFile.exists) {
				println("This application is already running (Or delete " + 
					pidFile.getAbsolutePath + " file).")
				System.exit(-1)
			}

			new FileOutputStream(pidFile).write(pid.getBytes)
			Runtime.getRuntime.addShutdownHook(new Thread {
				override def run {
					pidFile.delete()
				}
			})
		}
	}
}