package com.startune.music.utils

import com.atilika.kuromoji.CharacterDefinitions
import com.atilika.kuromoji.ConnectionCosts
import com.atilika.kuromoji.DoubleArrayTrie
import com.atilika.kuromoji.InsertedDictionary
import com.atilika.kuromoji.UnknownDictionary
import com.atilika.kuromoji.ipadic.TokenInfoDictionary
import com.atilika.kuromoji.ipadic.Tokenizer
import com.atilika.kuromoji.util.ResourceResolver
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** Resolves Kuromoji dictionary resources from an extracted directory
 * (downloaded on demand) instead of the APK's resources. Kuromoji asks for
 * plain basenames, so the name is used as-is. */
class FileResourceResolver(private val directory: File) : ResourceResolver {
    override fun resolve(resourceName: String): InputStream {
        val file = File(directory, resourceName.substringAfterLast('/'))
        if (!file.isFile) throw IOException("Dictionary file missing: ${file.name}")
        return FileInputStream(file)
    }
}

/**
 * Builds a Kuromoji [Tokenizer] whose dictionaries come from [directory].
 * Mirrors com.atilika.kuromoji.ipadic.Tokenizer.Builder.loadDictionaries()
 * (kuromoji 0.9.0) with the file-based resolver; penalties are the ipadic
 * defaults (kanji 2/3000, other 7/1700) and nakaguro split stays off, as in
 * the app's previous default Tokenizer() construction.
 */
class FileDictionaryTokenizerBuilder(private val directory: File) : Tokenizer.Builder() {
    override fun loadDictionaries() {
        penalties = arrayListOf(2, 3000, 7, 1700)
        resolver = FileResourceResolver(directory)
        try {
            doubleArrayTrie = DoubleArrayTrie.newInstance(resolver)
            connectionCosts = ConnectionCosts.newInstance(resolver)
            tokenInfoDictionary = TokenInfoDictionary.newInstance(resolver)
            characterDefinitions = CharacterDefinitions.newInstance(resolver)
            unknownDictionary = UnknownDictionary.newInstance(
                resolver, characterDefinitions, totalFeatures
            )
            insertedDictionary = InsertedDictionary(totalFeatures)
        } catch (e: Exception) {
            throw RuntimeException("Could not load dictionaries.", e)
        }
    }
}
