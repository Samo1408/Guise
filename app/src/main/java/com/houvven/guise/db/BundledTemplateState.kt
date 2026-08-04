package com.houvven.guise.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BundledTemplateState(
    @PrimaryKey val seedId: String,
    val templateId: String,
    val installedVersion: Int,
    val installedFingerprint: String,
    val deleted: Boolean,
    val managed: Boolean,
)
