package com.devson.vedlink.ui.presentation.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.devson.vedlink.domain.model.Link
import kotlin.collections.forEach

fun shareMultipleLinks(context: Context, links: List<Link>) {
    val shareText = buildString {
        appendLine("Check out these links:")
        appendLine()
        links.forEach { link ->
            if (!link.title.isNullOrBlank()) {
                appendLine(link.title)
            }
            appendLine(link.url)
            appendLine()
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_TITLE, "Shared Links from VedLink")
    }
    context.startActivity(Intent.createChooser(intent, "Share links via"))
}

fun shareLink(context: Context, url: String, title: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_TITLE, title ?: "Check out this link")
    }
    context.startActivity(Intent.createChooser(intent, "Share link via"))
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Link", text)
    clipboard.setPrimaryClip(clip)
}