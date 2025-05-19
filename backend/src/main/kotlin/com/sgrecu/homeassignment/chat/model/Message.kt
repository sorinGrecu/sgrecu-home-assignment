package com.sgrecu.homeassignment.chat.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

/**
 * Entity representing a message within a conversation.
 */
@Table("messages")
data class Message(
    @Id val id: Long? = null,

    @Column("conversation_id") val conversationId: UUID,

    @Column("role") val role: MessageRoleEnum,

    @Column("content") val content: String,

    @Column("created_at") @CreatedDate val createdAt: Instant? = null
) 