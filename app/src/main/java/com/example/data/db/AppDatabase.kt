package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "saved_documents")
data class SavedDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val uriString: String,
    val pageCount: Int,
    val extractedText: String,
    val summary: String,
    val quizJson: String = "",
    val audioScript: String = "",
    val videoPresentationJson: String = "",
    val userLvlProgress: Int = 0, // 0 = start, 1 = easy completed, 2 = medium completed, 3 = hard completed
    val addedAt: Long = System.currentTimeMillis(),
    val fileType: String = "PDF", // "PDF", "AUDIO", "VIDEO", "PHOTO"
    val subject: String = "", // e.g. "Cardiologia", "Fisiologia Humana"
    val topic: String = "", // e.g. "Insuficiência Cardíaca"
    val enrichedSources: String = "" // Comprehensive medical references & textbook info
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = SavedDocument::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val role: String, // "user" or "model"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO ---

@Dao
interface DocumentDao {

    @Query("SELECT * FROM saved_documents ORDER BY addedAt DESC")
    fun getAllDocuments(): Flow<List<SavedDocument>>

    @Query("SELECT * FROM saved_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Int): SavedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: SavedDocument): Long

    @Update
    suspend fun updateDocument(document: SavedDocument)

    @Query("DELETE FROM saved_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("SELECT * FROM chat_messages WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getChatMessagesForDocument(documentId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE documentId = :documentId")
    suspend fun clearChatMessagesForDocument(documentId: Int)
}

// --- Database ---

@Database(entities = [SavedDocument::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_ai_reader_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository ---

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<SavedDocument>> = documentDao.getAllDocuments()

    fun getChatMessagesFlow(documentId: Int): Flow<List<ChatMessage>> {
        return documentDao.getChatMessagesForDocument(documentId)
    }

    suspend fun getDocument(id: Int): SavedDocument? {
        return documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: SavedDocument): Int {
        return documentDao.insertDocument(document).toInt()
    }

    suspend fun updateDocument(document: SavedDocument) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(id: Int) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        documentDao.insertChatMessage(message)
    }

    suspend fun clearChat(documentId: Int) {
        documentDao.clearChatMessagesForDocument(documentId)
    }
}
