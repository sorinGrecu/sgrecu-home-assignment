package com.sgrecu.homeassignment.chat.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

/**
 * Entity representing a conversation between a user and the AI.
 */
@Table("conversations")
data class Conversation(
    @Id val id: UUID? = null,

    @Column("user_id") val userId: String,

    @Column("title") val title: String? = null,

    @Column("created_at") @CreatedDate val createdAt: Instant? = null,

    @Column("updated_at") @LastModifiedDate val updatedAt: Instant? = null
) 