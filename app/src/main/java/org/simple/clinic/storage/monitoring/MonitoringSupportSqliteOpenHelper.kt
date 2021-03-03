package org.simple.clinic.storage.monitoring

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import timber.log.Timber
import java.util.Locale

class MonitoringSupportSqliteOpenHelper(
    context: Context,
    private val wrapped: SupportSQLiteOpenHelper
) : SupportSQLiteOpenHelper {

  private val daoMetadata: Map<String, DaoMetadata>

  init {
    val assetManager = context.assets
    val metadata = assetManager
        .open("db_metadata.csv")
        .reader()
        .readText()

    daoMetadata = DaoMetadata.parse(metadata)
  }

  override fun close() {
    wrapped.close()
  }

  override fun getDatabaseName() = wrapped.databaseName

  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    wrapped.setWriteAheadLoggingEnabled(enabled)
  }

  override fun getWritableDatabase(): SupportSQLiteDatabase {
    return MonitoringSupportSqliteDatabase(wrapped.writableDatabase)
  }

  override fun getReadableDatabase(): SupportSQLiteDatabase {
    return MonitoringSupportSqliteDatabase(wrapped.readableDatabase)
  }

  class Factory(
      private val context: Context,
      private val wrapped: SupportSQLiteOpenHelper.Factory
  ) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ) = MonitoringSupportSqliteOpenHelper(context, wrapped.create(configuration))
  }

  class MonitoringSupportSqliteDatabase(
      private val wrapped: SupportSQLiteDatabase
  ) : SupportSQLiteDatabase {
    override fun close() {
      wrapped.close()
    }

    override fun compileStatement(sql: String?): SupportSQLiteStatement = wrapped.compileStatement(sql)

    override fun beginTransaction() {
      wrapped.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
      wrapped.beginTransactionNonExclusive()
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener?) {
      wrapped.beginTransactionWithListener(transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener?) {
      wrapped.beginTransactionWithListenerNonExclusive(transactionListener)
    }

    override fun endTransaction() {
      wrapped.endTransaction()
    }

    override fun setTransactionSuccessful() {
      wrapped.setTransactionSuccessful()
    }

    override fun inTransaction() = wrapped.inTransaction()

    override fun isDbLockedByCurrentThread() = wrapped.isDbLockedByCurrentThread

    override fun yieldIfContendedSafely() = wrapped.yieldIfContendedSafely()

    override fun yieldIfContendedSafely(sleepAfterYieldDelay: Long) = wrapped.yieldIfContendedSafely(sleepAfterYieldDelay)

    override fun getVersion() = wrapped.version

    override fun setVersion(version: Int) {
      wrapped.version = version
    }

    override fun getMaximumSize() = wrapped.maximumSize

    override fun setMaximumSize(numBytes: Long) = wrapped.setMaximumSize(numBytes)

    override fun getPageSize() = wrapped.pageSize

    override fun setPageSize(numBytes: Long) {
      wrapped.pageSize = numBytes
    }

    override fun query(query: String?): Cursor {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.query(query)
    }

    override fun query(query: String?, bindArgs: Array<out Any>?): Cursor {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.query(query, bindArgs)
    }

    override fun query(query: SupportSQLiteQuery?): Cursor {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.query(query)
    }

    override fun query(query: SupportSQLiteQuery?, cancellationSignal: CancellationSignal?): Cursor {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.query(query, cancellationSignal)
    }

    override fun insert(table: String?, conflictAlgorithm: Int, values: ContentValues?): Long {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.insert(table, conflictAlgorithm, values)
    }

    override fun delete(table: String?, whereClause: String?, whereArgs: Array<out Any>?): Int {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.delete(table, whereClause, whereArgs)
    }

    override fun update(table: String?, conflictAlgorithm: Int, values: ContentValues?, whereClause: String?, whereArgs: Array<out Any>?): Int {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      return wrapped.update(table, conflictAlgorithm, values, whereClause, whereArgs)
    }

    override fun execSQL(sql: String?) {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      wrapped.execSQL(sql)
    }

    override fun execSQL(sql: String?, bindArgs: Array<out Any>?) {
      val throwable = Throwable()
      throwable.fillInStackTrace()
      Timber.tag("SearchPerf").i(throwable)
      wrapped.execSQL(sql, bindArgs)
    }

    override fun isReadOnly() = wrapped.isReadOnly

    override fun isOpen() = wrapped.isOpen

    override fun needUpgrade(newVersion: Int) = wrapped.needUpgrade(newVersion)

    override fun getPath(): String = wrapped.path

    override fun setLocale(locale: Locale?) {
      wrapped.setLocale(locale)
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
      wrapped.setMaxSqlCacheSize(cacheSize)
    }

    override fun setForeignKeyConstraintsEnabled(enable: Boolean) {
      wrapped.setForeignKeyConstraintsEnabled(enable)
    }

    override fun enableWriteAheadLogging() = wrapped.enableWriteAheadLogging()

    override fun disableWriteAheadLogging() {
      wrapped.disableWriteAheadLogging()
    }

    override fun isWriteAheadLoggingEnabled() = wrapped.isWriteAheadLoggingEnabled

    override fun getAttachedDbs(): MutableList<Pair<String, String>> = wrapped.attachedDbs

    override fun isDatabaseIntegrityOk() = wrapped.isDatabaseIntegrityOk
  }

  data class DaoMetadata(
      val daoName: String,
      val methodName: String,
      val start: Int,
      val end: Int
  ) {

    companion object {
      fun parse(csv: String): Map<String, DaoMetadata> {
        return csv
            .split('\n')
            .asSequence()
            .filterNot { it.isBlank() }
            .map { it.split(',') }
            .map { (dao, method, start, end) -> DaoMetadata(dao, method, start.toInt(), end.toInt()) }
            .associateBy { "${it.daoName}.${it.methodName}" }
      }
    }
  }
}
