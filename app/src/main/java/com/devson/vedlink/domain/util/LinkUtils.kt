package com.devson.vedlink.domain.util

import android.content.Context
import android.content.Intent
import com.devson.vedlink.domain.model.Link

object LinkUtils {

    /**
     * Share a single link with formatted message
     */
    fun shareSingleLink(context: Context, link: Link) {
        val shareText = "Check Out this link ${link.url}"
        shareText(context, shareText)
    }

    /**
     * Share multiple links with formatted message
     */
    fun shareMultipleLinks(context: Context, links: List<Link>) {
        if (links.isEmpty()) return

        if (links.size == 1) {
            shareSingleLink(context, links.first())
            return
        }

        val shareText = buildString {
            append("Check Out These Links:\n")
            links.forEach { link ->
                append("${link.url}\n")
            }
        }.trim()

        shareText(context, shareText)
    }

    /**
     * Share text using Android's share sheet
     */
    private fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(intent, "Share Link")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share URLs directly without Link objects
     */
    fun shareUrls(context: Context, urls: List<String>) {
        if (urls.isEmpty()) return

        val shareText = if (urls.size == 1) {
            "Check Out this link ${urls.first()}"
        } else {
            buildString {
                append("Check Out These Links:\n")
                urls.forEach { url ->
                    append("$url\n")
                }
            }.trim()
        }

        shareText(context, shareText)
    }
}