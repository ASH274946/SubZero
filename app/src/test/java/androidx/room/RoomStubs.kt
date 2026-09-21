package androidx.room

import android.content.Context
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Entity(val tableName: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryKey(val autoGenerate: Boolean = false)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Dao

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Insert(val onConflict: Int = 0)

object OnConflictStrategy {
    const val REPLACE = 1
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Database(
    val entities: Array<KClass<*>>,
    val version: Int,
    val exportSchema: Boolean = true
)

abstract class RoomDatabase {
    class Builder<T : RoomDatabase>(
        private val context: Context,
        private val klass: Class<T>,
        private val name: String?
    ) {
        fun fallbackToDestructiveMigration(): Builder<T> = this
        fun build(): T = klass.getDeclaredConstructor().newInstance()
    }
}

object Room {
    fun <T : RoomDatabase> databaseBuilder(
        context: Context,
        klass: Class<T>,
        name: String?
    ): RoomDatabase.Builder<T> = RoomDatabase.Builder(context, klass, name)
}
