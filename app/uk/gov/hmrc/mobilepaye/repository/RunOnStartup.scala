package uk.gov.hmrc.mobilepaye.repository


import com.google.inject.Inject
import play.api.Logger

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RunOnStartup @Inject() (p800CacheMongo: P800CacheMongo)(implicit executionContext: ExecutionContext) {
  val logger: Logger = Logger(this.getClass)

  val resetCache: Future[Unit] = for {
    deleteCache <- p800CacheMongo.deleteCacheRecords()
  } yield (logger.info(
    s"\n====================== P800 CACHE DELETED ======================\n\nSuccess = ${deleteCache.wasAcknowledged()}\nRecords removed = ${deleteCache.getDeletedCount}\n\n========================================================================================"
  ))

  resetCache.recover {
    case e => logger.warn("Resetting P800 cache failed: " + e)
  }

}
