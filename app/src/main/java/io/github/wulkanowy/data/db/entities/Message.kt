package io.github.wulkanowy.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.time.Instant

@Entity(tableName = "Messages")
data class Message(

    @ColumnInfo(name = "student_id")
    val studentId: Long,

    @ColumnInfo(name = "real_id")
    val realId: Int,

    @ColumnInfo(name = "message_id")
    val messageId: Int,

    @ColumnInfo(name = "sender_name")
    val sender: String,

    @ColumnInfo(name = "sender_id")
    val senderId: Int,

    @ColumnInfo(name = "recipient_name")
    val recipient: String,

    val subject: String,

    val date: Instant,

    @ColumnInfo(name = "folder_id")
    val folderId: Int,

    var unread: Boolean,

    val removed: Boolean,

    @ColumnInfo(name = "has_attachments")
    val hasAttachments: Boolean
) : Serializable {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "is_notified")
    var isNotified: Boolean = true

    @ColumnInfo(name = "unread_by")
    var unreadBy: Int = 0

    @ColumnInfo(name = "read_by")
    var readBy: Int = 0

    var content: String = ""
}
