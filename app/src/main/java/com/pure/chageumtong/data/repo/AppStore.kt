package com.pure.chageumtong.data.repo

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private val json = Json { ignoreUnknownKeys = true }

private object AppDataSerializer : Serializer<AppData> {
    override val defaultValue = AppData()

    override suspend fun readFrom(input: InputStream): AppData = try {
        json.decodeFromString(AppData.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("app_data.json 읽기 실패", e)
    }

    override suspend fun writeTo(t: AppData, output: OutputStream) {
        output.write(json.encodeToString(AppData.serializer(), t).encodeToByteArray())
    }
}

/** DataStore 파일 하나 (app_data.json). 깨지면 빈 데이터로 교체 → 시드가 다시 들어간다. */
class AppStore(context: Context, scope: CoroutineScope) {
    private val store: DataStore<AppData> = DataStoreFactory.create(
        serializer = AppDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { AppData() },
        scope = scope,
        produceFile = { context.dataStoreFile("app_data.json") },
    )

    val data: Flow<AppData> = store.data

    suspend fun update(transform: (AppData) -> AppData): AppData = store.updateData(transform)
}
