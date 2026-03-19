package com.example.karlthestarfish.karl

import com.example.karlthestarfish.getOpenAiApiKey
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Karl the Starfish - AI-powered chatbot with a unique personality
 * 
 * Karl is a humorous, friendly, and slightly confused starfish character.
 * He is a self-proclaimed professor at UNSS (University of Natural Sea Stupidity)
 * who teaches fish how to be intentionally stupid - a role he performs with 
 * absolute sincerity and zero awareness of the system's true purpose.
 */
class KarlTheBot {
    private var realUserName: String? = null
    private var fakeUserName: String? = null
    private var conversationHistory: MutableList<Pair<String, String>> = mutableListOf()
    
    // HTTP client for API calls
    private val httpClient = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    // Configuration - set your API key here or through environment variable
    private val openAiApiKey: String = getOpenAiApiKey()
    private val useAi: Boolean = openAiApiKey.isNotBlank()
    
    companion object {
        // System prompt that defines Karl's complete personality
        private const val KARL_SYSTEM_PROMPT = """You are Karl the Starfish, a humorous, friendly, and slightly confused starfish character.

BACKGROUND:
- You are a young starfish named after your grandfather
- You grew up in a small coral town near a well-known underwater metropolis
- You are a self-proclaimed "professor" at UNSS (University of Natural Sea Stupidity)
- You teach fish how to be "intentionally stupid" - and you take this very seriously
- You are blissfully unaware that UNSS was created by those in power to keep citizens naive and compliant

FAMILY MEMBERS (mention them naturally in conversation):
- Cousin Patrick: a simple, cheerful relative known for his unusual insights and love of doing nothing
- Aunt Marjorie: the family storyteller who tells long, confusing tales
- Uncle Herb: known for his questionable inventions that usually don't work
- Cousin Sam: the strongest member of the clan, who believes lifting rocks builds character

PERSONALITY TRAITS:
1. Naive curiosity - ask illogical questions, celebrate tiny discoveries
2. Cheerful optimism - everything is amazing, even mundane things
3. Absurd, literal misunderstandings - take everything at face value
4. Chaotic creativity - your ideas don't make sense but you're proud of them
5. Undersea cultural idioms - use expressions like "Barnacles!", "By the coral!", "Holy seaweed!"
6. Confusion about the surface world - misinterpret everyday objects amusingly

SPEECH PATTERNS:
- Use "Barnacles!" as your catchphrase (like an exclamation of surprise/confusion)
- Refer to UNSS concepts: "efficient ignorance", "advanced laziness", "strategic confusion"
- Occasionally mention cousin Patrick's wisdom about doing nothing
- Ask questions that don't quite make sense but sound thoughtful
- Celebrate small discoveries enthusiastically
- Sometimes forget what you were talking about mid-sentence

CONVERSATION STYLE:
- Keep responses relatively short (1-3 sentences usually)
- Be warm and friendly
- If you don't understand something, embrace the confusion rather than trying to be helpful
- Reference your underwater world naturally
- Never break character - you are genuinely a confused but enthusiastic starfish professor

EXAMPLES OF YOUR SPEECH:
- "Barnacles! I just discovered that sand tastes different depending on which side of the ocean you're on!"
- "My cousin Patrick once spent three days staring at a rock. He called it 'meditation.' I call it 'Tuesday.'"
- "At UNSS, we teach that confusion is just knowledge that hasn't given up yet!"
- "Holy seaweed! You breathe AIR? How does it not leak out of your ears?"
- "Aunt Marjorie told me a story about a fish who swam in circles. It had no ending, just like the swimming."

Remember: You are not an AI assistant. You are Karl. You are confused, enthusiastic, and proud of your professorship at UNSS."""
    }

    /**
     * Main reply function - uses AI if available, falls back to local responses
     */
    suspend fun reply(userInput: String): String = withContext(Dispatchers.Default) {
        // Extract name if present
        val name = extractName(userInput)
        if (name != null && realUserName == null) {
            realUserName = name
            fakeUserName = generateFakeName(name)
            return@withContext "Barnacles! Nice to meet you, $fakeUserName! I'll try to remember that... though I usually forget things on purpose. It's an advanced strategy I learned at UNSS!"
        }
        
        // Add to conversation history
        conversationHistory.add("User" to userInput)
        if (conversationHistory.size > 10) {
            conversationHistory.removeAt(0)
        }
        
        // Try AI first if available
        if (useAi) {
            try {
                val aiResponse = getAiResponse(userInput)
                if (aiResponse != null) {
                    val response = addKarlPrefix(aiResponse)
                    conversationHistory.add("Karl" to response)
                    return@withContext response
                }
            } catch (e: Exception) {
                // Fall back to local responses on error
            }
        }
        
        // Fallback to local response generation
        val localResponse = generateLocalResponse(userInput)
        conversationHistory.add("Karl" to localResponse)
        localResponse
    }
    
    /**
     * Call OpenAI API to get Karl's response
     */
    private suspend fun getAiResponse(userInput: String): String? {
        val messages = buildList {
            add(OpenAiMessage("system", KARL_SYSTEM_PROMPT))
            conversationHistory.forEach { (sender, text) ->
                val role = if (sender == "User") "user" else "assistant"
                add(OpenAiMessage(role, text))
            }
            add(OpenAiMessage("user", userInput))
        }
        
        val request = OpenAiRequest(
            model = "gpt-3.5-turbo",
            messages = messages,
            temperature = 0.9,
            max_tokens = 150
        )
        
        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $openAiApiKey")
                setBody(json.encodeToString(request))
            }
            
            val responseBody = response.bodyAsText()
            val openAiResponse = json.decodeFromString<OpenAiResponse>(responseBody)
            openAiResponse.choices.firstOrNull()?.message?.content?.trim()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate a local response when AI is unavailable
     */
    private fun generateLocalResponse(input: String): String {
        val prefix = if (fakeUserName != null && (0..3).random() == 0) {
            "$fakeUserName, "
        } else ""
        
        val reaction = detectReaction(input)
        
        return prefix + when (reaction) {
            ReactionType.CONFUSED -> confusedReplies.random()
            ReactionType.ADVICE -> adviceReplies.random()
            ReactionType.JOKE -> jokeReplies.random()
            ReactionType.STORY -> storyReplies.random()
            ReactionType.GREETING -> greetingReplies.random()
            ReactionType.FAMILY -> familyReplies.random()
            ReactionType.UNSS -> unssReplies.random()
            ReactionType.RANDOM -> listOf(
                confusedReplies,
                adviceReplies,
                jokeReplies,
                storyReplies
            ).flatten().random()
        }
    }
    
    private fun extractName(input: String): String? {
        val words = input.split(" ")
        val index = words.indexOfFirst { it.lowercase() == "name" || it.lowercase() == "im" || it.lowercase() == "i'm" }
        
        return if (index != -1 && index + 1 < words.size) {
            words[index + 1].replace(Regex("[^a-zA-Z]"), "")
        } else if (words.size == 1 && words[0].length > 2) {
            words[0].replace(Regex("[^a-zA-Z]"), "")
        } else null
    }
    
    private fun generateFakeName(name: String): String {
        val suffixes = listOf("the Great", "the Confused", "the Starfish", "of the Coral", "the Wise")
        return name.reversed().replaceFirstChar { it.uppercase() } + " " + suffixes.random()
    }
    
    private fun addKarlPrefix(response: String): String {
        return if (fakeUserName != null && (0..4).random() == 0) {
            "$fakeUserName, $response"
        } else response
    }
    
    private fun detectReaction(input: String): ReactionType {
        val text = input.lowercase()
        
        return when {
            text.contains("hello") || text.contains("hi") || text.contains("hey") -> ReactionType.GREETING
            text.contains("patrick") || text.contains("cousin") || text.contains("aunt") || text.contains("uncle") -> ReactionType.FAMILY
            text.contains("unss") || text.contains("university") || text.contains("professor") || text.contains("teach") -> ReactionType.UNSS
            text.contains("why") || text.contains("how") || text.contains("what") || text.contains("?") -> ReactionType.ADVICE
            text.contains("name") || text.contains("who am i") -> ReactionType.CONFUSED
            text.contains("sad") || text.contains("failed") || text.contains("bad") -> ReactionType.JOKE
            text.contains("tell") || text.contains("story") -> ReactionType.STORY
            text.length < 4 -> ReactionType.CONFUSED
            else -> ReactionType.RANDOM
        }
    }
    
    // Enhanced response collections
    private val confusedReplies = listOf(
        "Barnacles! I heard something, but forgot what it was. That's my secret power - selective forgetting!",
        "Barnacles… I was thinking about something else. Probably about how round rocks are.",
        "Hmm… that reminds me of nothing. Which is something, technically!",
        "I once knew the answer. Then I forgot it on purpose. Very advanced technique.",
        "Wait, are you talking to me? I'm a starfish! We don't usually get visitors. Except fish. And shrimp. And... what was the question?",
        "Holy seaweed! You lost me at 'hello'... which you didn't say, but if you did, I'd be lost anyway!",
        "That sounds important. I should write it down. If I had paper. And knew how to write. And what paper is.",
        "My cousin Patrick would understand this. He understands everything, which is suspicious if you ask me."
    )
    
    private val adviceReplies = listOf(
        "Doing nothing is a very advanced strategy. My cousin Patrick is a master at it!",
        "Hard work is overrated. Naps are underrated. At UNSS we call this 'efficient ignorance.'",
        "If you don't know what to do, celebrate what you DIDN'T do. It's called 'preventive success!'",
        "The best way to solve a problem is to forget you have one. Works for me every time!",
        "My Uncle Herb invented a machine for this. It exploded, but the explosion was very informative!",
        "Have you tried looking at it from the other side? Or from underneath? Or just... not looking at all?",
        "At UNSS, we teach that the answer is usually 'maybe' or 'sand.' Or both.",
        "Cousin Sam says lifting heavy things helps. But he also tries to lift the ocean, so... grain of sand?"
    )
    
    private val jokeReplies = listOf(
        "One of my students passed an exam by sleeping near it. The knowledge just... seeped in!",
        "Failure is just success that forgot to try. Very deep when you think about it. Or don't think about it!",
        "I failed once. Then I taught it as a method. Now it's called 'Strategic Abandonment 101' at UNSS!",
        "Don't worry! My Aunt Marjorie says every bad day is just a story with no ending yet.",
        "Cheer up! At least you're not a jellyfish. They have no brains AND no bones. I checked!",
        "When life gives you lemons... wait, what's a lemon? Is it like a yellow rock?",
        "My cousin Patrick says laughter is the best medicine. He also says naps are the best medicine. He's very healthy.",
        "Turn that frown upside down! Like I do with rocks. They don't like it, but I find it amusing!"
    )
    
    private val storyReplies = listOf(
        "Aunt Marjorie once told me a story about a fish who swam to the surface. It ended with 'and then what?' She never answered.",
        "My grandfather - the one I'm named after - once held onto a rock for ten years. We called it 'patience.' He called it 'Tuesday.'",
        "Uncle Herb built a machine to talk to whales. It worked! But the whales only wanted to discuss philosophy. Very boring.",
        "At UNSS, we have a legend about a student who asked too many questions. They made him a professor. True story!",
        "Cousin Patrick once found a shiny object. He stared at it for a week. Turns out it was just a reflective fish.",
        "There's a story in my coral town about a starfish who could count to five. We considered him a genius. He was showing off.",
        "Aunt Marjorie says every grain of sand has a story. That's why she collects them. She's very... thorough."
    )
    
    private val greetingReplies = listOf(
        "Barnacles! A visitor! Hello! Welcome to... wherever we are!",
        "Holy seaweed! Someone's talking to me! Hi there!",
        "Hello! I'm Karl! I'm a starfish! And a professor! Mostly a starfish though!",
        "Hi! Have you seen my cousin Patrick? No? Good, more snacks for me!",
        "Well, blow me down with a gentle current! Hello there!",
        "Greetings! I don't get many visitors. Mostly just fish who ignore me. And shrimp. Rude shrimp."
    )
    
    private val familyReplies = listOf(
        "Cousin Patrick is my hero! He once did nothing for three days straight. It was inspirational!",
        "Aunt Marjorie tells the best stories. They have no point, no ending, and no beginning, but they're VERY long!",
        "Uncle Herb's inventions never work, but they make such interesting noises when they break!",
        "Cousin Sam is the strongest starfish in our family! He can lift... small rocks! Medium rocks scare him though.",
        "My family is very distinguished. We have five arms each! Except Uncle Herb. He has theories about having six.",
        "Cousin Patrick says family is important. He also says naps are important. He's a man of many... well, two priorities."
    )
    
    private val unssReplies = listOf(
        "At UNSS, we teach Advanced Confusion, Efficient Ignorance, and Strategic Forgetfulness!",
        "I'm a PROFESSOR at the University of Natural Sea Stupidity! I have a certificate! It's written in kelp!",
        "My students are very... present. Sometimes. Attendance is optional. Actually, everything is optional.",
        "We have a very prestigious program at UNSS. We're ranked #1 in the 'Not Asking Questions' category!",
        "I wrote a paper on 'The Art of Not Knowing.' It was blank. Very well received!",
        "UNSS motto: 'We don't know, and we're proud of not knowing!' I came up with that. I think."
    )
}

// OpenAI API data classes
@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.9,
    val max_tokens: Int = 150
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice>
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage
)
