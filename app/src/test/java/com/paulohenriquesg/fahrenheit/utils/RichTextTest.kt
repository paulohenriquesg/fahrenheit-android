package com.paulohenriquesg.fahrenheit.utils

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Podcast descriptions arrive as HTML and were printed as they came, tags and
 * numeric entities included (#57). They are rendered now rather than flattened,
 * so what the publisher emphasised stays emphasised.
 */
@RunWith(RobolectricTestRunner::class)
class RichTextTest {

    @Test
    fun `tags stop being words`() {
        assertEquals("About it", RichText.fromHtml("<p>About it</p>").text)
    }

    @Test
    fun `entities become the characters they stand for`() {
        assertEquals("Fish & Chips", RichText.fromHtml("Fish &amp; Chips").text)
        assertEquals("it's", RichText.fromHtml("it&#39;s").text)
    }

    @Test
    fun `a line break is a line break, not a tag`() {
        assertEquals("one\ntwo", RichText.fromHtml("one<br>two").text)
    }

    @Test
    fun `paragraphs stay apart`() {
        // Compose separates them with one newline, not a blank line.
        assertEquals("one\ntwo", RichText.fromHtml("<p>one</p><p>two</p>").text)
    }

    @Test
    fun `bold stays bold`() {
        val rendered = RichText.fromHtml("plain <b>loud</b>")

        assertEquals("plain loud", rendered.text)
        val bold = rendered.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals("loud", rendered.text.substring(bold.single().start, bold.single().end))
    }

    @Test
    fun `italics stay italic`() {
        val rendered = RichText.fromHtml("plain <i>leaning</i>")

        val italic = rendered.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals("leaning", rendered.text.substring(italic.single().start, italic.single().end))
    }

    @Test
    fun `a link keeps its words and loses its markup`() {
        val rendered = RichText.fromHtml("""Listen on <a href="https://example.com">Spotify</a>""")

        assertEquals("Listen on Spotify", rendered.text)
    }

    @Test
    fun `text that was never HTML is left alone`() {
        assertEquals(
            "A man. His ex-girlfriend's cat.",
            RichText.fromHtml("A man. His ex-girlfriend's cat.").text
        )
    }

    @Test
    fun `nothing in, nothing out`() {
        assertEquals("", RichText.fromHtml(null).text)
        assertEquals("", RichText.fromHtml("").text)
    }

    @Test
    fun `no stray blank lines at either end`() {
        assertTrue(RichText.fromHtml("<p>About it</p>\n\n").text == "About it")
    }

    @Test
    fun `an ampersand that was only escaped once still reads as one`() {
        assertEquals("Fish & Chips", RichText.fromHtml("Fish &amp; Chips").text)
    }

    @Test
    fun `text that merely mentions an ampersand is left alone`() {
        assertEquals("rock & roll", RichText.fromHtml("rock & roll").text)
    }

    // #194: some descriptions hold the two characters backslash and n, not a newline.
    @Test
    fun `a written backslash-n is a line break`() {
        assertEquals("one\ntwo", RichText.fromHtml("one\\ntwo").text)
        assertEquals("one\ntwo", RichText.fromHtml("one\\r\\ntwo").text)
    }

    @Test
    fun `two written backslash-n are a paragraph break`() {
        assertEquals("one\n\ntwo", RichText.fromHtml("one\\n\\ntwo").text)
        assertEquals("one\n\ntwo", RichText.fromHtml("one\\r\\n\\r\\ntwo").text)
    }

    @Test
    fun `an HTML description reads as it did`() {
        val rendered = RichText.fromHtml("<p>A <b>bold</b> start.</p><p>Then more.</p>")
        assertEquals("A bold start.\nThen more.", rendered.text)
        assertEquals(1, rendered.spanStyles.count { it.item.fontWeight == FontWeight.Bold })
    }
}
