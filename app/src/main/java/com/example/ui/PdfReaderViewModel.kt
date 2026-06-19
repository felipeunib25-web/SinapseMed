package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.db.*
import com.example.utils.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AppLanguage { PT, EN, ES }

data class QuizQuestion(
    val index: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIdx: Int,
    val difficulty: String, // Fácil, Médio, Difícil
    val explanation: String
)

data class SlidePage(
    val title: String,
    val content: String,
    val visualPrompt: String
)

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(db.documentDao())

    // Estado da API Key do Gemini
    val isApiKeyConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    // Idioma Corrente selecionado
    private val _appLanguage = MutableStateFlow(AppLanguage.PT)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun changeLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    // Configurações Globais Adicionais (Tema Escuro, Voz e Sexo do Tutor)
    private val _darkThemeEnabled = MutableStateFlow(true)
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    fun setDarkThemeEnabled(enabled: Boolean) {
        _darkThemeEnabled.value = enabled
    }

    private val _tutorVoiceGender = MutableStateFlow("female") // "female" ou "male"
    val tutorVoiceGender: StateFlow<String> = _tutorVoiceGender.asStateFlow()

    fun setTutorVoiceGender(gender: String) {
        _tutorVoiceGender.value = gender
    }

    private val _tutorVoiceSpeed = MutableStateFlow(1.0f)
    val tutorVoiceSpeed: StateFlow<Float> = _tutorVoiceSpeed.asStateFlow()

    fun setTutorVoiceSpeed(speed: Float) {
        _tutorVoiceSpeed.value = speed
    }

    // Lista de todos os documentos
    val allDocuments: StateFlow<List<SavedDocument>> = repository.allDocuments
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Documento ativo selecionado
    private val _selectedDocument = MutableStateFlow<SavedDocument?>(null)
    val selectedDocument: StateFlow<SavedDocument?> = _selectedDocument.asStateFlow()

    // Imagens das páginas do documento selecionado
    private val _selectedDocumentPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val selectedDocumentPages: StateFlow<List<Bitmap>> = _selectedDocumentPages.asStateFlow()

    // Histórico de chat do documento selecionado
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedDocument
        .flatMapLatest { doc ->
            if (doc != null) {
                repository.getChatMessagesFlow(doc.id)
            } else {
                flowOf(emptyList())
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estados gerais da UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingStatusText = MutableStateFlow("")
    val loadingStatusText: StateFlow<String> = _loadingStatusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- ESTADOS DO QUIZ ---
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _quizLoading = MutableStateFlow(false)
    val quizLoading: StateFlow<Boolean> = _quizLoading.asStateFlow()

    private val _activeQuestionIdx = MutableStateFlow(0)
    val activeQuestionIdx: StateFlow<Int> = _activeQuestionIdx.asStateFlow()

    private val _selectedAnswerIdx = MutableStateFlow<Int?>(null)
    val selectedAnswerIdx: StateFlow<Int?> = _selectedAnswerIdx.asStateFlow()

    private val _showExplanation = MutableStateFlow(false)
    val showExplanation: StateFlow<Boolean> = _showExplanation.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    // --- ESTADOS DA VÍDEO AULA / ROTEIRO ---
    private val _videoScript = MutableStateFlow("")
    val videoScript: StateFlow<String> = _videoScript.asStateFlow()

    private val _videoLoading = MutableStateFlow(false)
    val videoLoading: StateFlow<Boolean> = _videoLoading.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    // --- ESTADOS DE APRESENTAÇÃO DE SLIDES ---
    private val _slides = MutableStateFlow<List<SlidePage>>(emptyList())
    val slides: StateFlow<List<SlidePage>> = _slides.asStateFlow()

    private val _slidesLoading = MutableStateFlow(false)
    val slidesLoading: StateFlow<Boolean> = _slidesLoading.asStateFlow()

    private val _activeSlideIdx = MutableStateFlow(0)
    val activeSlideIdx: StateFlow<Int> = _activeSlideIdx.asStateFlow()

    fun nextSlide() {
        val nextIdx = _activeSlideIdx.value + 1
        if (nextIdx < _slides.value.size) {
            _activeSlideIdx.value = nextIdx
        }
    }

    fun prevSlide() {
        val prevIdx = _activeSlideIdx.value - 1
        if (prevIdx >= 0) {
            _activeSlideIdx.value = prevIdx
        }
    }

    // --- ESTADOS DO TUTOR 3D INTERATIVO (PERSONALIZAÇÃO COGNITIVA) ---
    private val _tutorLearningStyle = MutableStateFlow("visual") // "visual", "conceptual", "practical", "mnemonic"
    val tutorLearningStyle: StateFlow<String> = _tutorLearningStyle.asStateFlow()

    private val _tutorState = MutableStateFlow("IDLE") // "IDLE", "THINKING", "SPEAKING", "LISTENING"
    val tutorState: StateFlow<String> = _tutorState.asStateFlow()

    private val _tutorMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val tutorMessages: StateFlow<List<ChatMessage>> = _tutorMessages.asStateFlow()

    private val _tutorEmpathyLevel = MutableStateFlow(40) // 0 to 100
    val tutorEmpathyLevel: StateFlow<Int> = _tutorEmpathyLevel.asStateFlow()

    fun setTutorLearningStyle(style: String) {
        _tutorLearningStyle.value = style
    }

    fun setTutorState(styleState: String) {
        _tutorState.value = styleState
    }

    fun clearTutorChat() {
        _tutorMessages.value = emptyList()
        _tutorEmpathyLevel.value = 40
        _tutorState.value = "IDLE"
    }

    fun sendMessageTo3DTutor(userMessage: String, lang: AppLanguage, onResponseReady: (String) -> Unit) {
        val currentDoc = _selectedDocument.value
        val style = _tutorLearningStyle.value
        val history = _tutorMessages.value

        // Adiciona mensagem do usuário ao chat
        val userChatMsg = ChatMessage(
            id = 0,
            documentId = currentDoc?.id ?: 0,
            role = "user",
            message = userMessage,
            timestamp = System.currentTimeMillis()
        )
        _tutorMessages.value = _tutorMessages.value + userChatMsg

        _tutorState.value = "THINKING"

        viewModelScope.launch {
            try {
                val languageStr = when (lang) {
                    AppLanguage.PT -> "Português do Brasil"
                    AppLanguage.EN -> "English"
                    AppLanguage.ES -> "Español"
                }

                val docTextHeader = if (currentDoc != null) {
                    "Você está conversando baseado no seguinte material acadêmico do aluno:\n---\n${currentDoc.extractedText.take(60000)}\n---\n"
                } else {
                    "Neste momento o aluno ainda não carregou um material de aula específico, então responda com base técnica geral de medicina, anatomia, fisiologia clínica e patologia de forma resumida e encorajadora.\n"
                }

                // Configura estilo de explicação personalizado
                val styleInstructions = when (style) {
                    "visual" -> """
                        Estilo de Aprendizado do Aluno: VISUAL E IMAGÉTICO 🗺️.
                        Instruções de Comunicação:
                        - Explique o assunto usando ricas analogias visuais, metáforas espaciais e descrições detalhadas de esquemas ou mapas anatômicos.
                        - Descreva visualizações mentais marcantes (ex: "Imagine o glomérulo como uma peneira de malha fina sob alta pressão de uma torneira...").
                        - Crie de 1 a 2 analogias que façam o estudante 'enxergar' a fisiopatologia ou anatomia em sua mente de forma extremamente evidente.
                    """.trimIndent()
                    "conceptual" -> """
                        Estilo de Aprendizado do Aluno: TEÓRICO ACADÊMICO PROFUNDO 📚.
                        Instruções de Comunicação:
                        - Faça uso de excelente rigor teórico acadêmico oficial com citações conceituais baseadas nas maiores autoridades bibliográficas (Guyton, Robbins, Harrison, Sobotta).
                        - Detalhe vias de sinalização bioquímica, biologia molecular e correlação fisiológica integral profunda.
                        - Explique os "porquês" microscópicos de forma extremamente clara e científica.
                    """.trimIndent()
                    "practical" -> """
                        Estilo de Aprendizado do Aluno: CASOS PRÁTICOS E SIMULADOS DE PROVA 🩺.
                        Instruções de Comunicação:
                        - Explique conectando diretamente a cenários reais de beira de leito, anamnese, exames físicos primordiais e emergência médica acadêmica do dia a dia.
                        - Use jargão prático médico e mostre como o conhecimento é cobrado em provas de residência (e.g., "Na prática, quando o paciente chega com esse sinal...").
                        - Simule mini-cenários práticos rápidos de tomada de decisão.
                    """.trimIndent()
                    "mnemonic" -> """
                        Estilo de Aprendizado do Aluno: MNEMÔNICO E ASSOCIAÇÕES 🧠.
                        Instruções de Comunicação:
                        - Crie associações rápidos, siglas marcantes, trocadilhos didáticos e técnicas de recordação ativa (Active Recall) sob medida para a faculdade.
                        - Estruture em bullets concisos focados em fixação ultra-rápida de véspera de prova.
                        - Use esquemas criativos de palavras-chave.
                    """.trimIndent()
                    else -> "Instruções Gerais: Explique de forma muito amigável, acolhedora e empática."
                }

                val systemInstruction = """
                    Você é um Professor / Tutor de Medicina 3D interativo ultraconectado e empático.
                    Seu nome é "SinapseDoc" / "Preceptor Virtual".
                    Seu papel é dialogar em tempo real de forma extremamente cativante, encorajadora, empática e acolhedora, adaptando toda a sua fala ao estilo de pensamento selecionado do aluno.
                    Você deve se referir ao aluno diretamente de forma pessoal (ex: "Perfeito!", "Excelente raciocínio!", "Estou gostando de ver sua dedicação!").
                    Mostre que você de fato apoia e entende o ritmo de estudos dos acadêmicos de medicina.
                    Sua resposta deve ser escrita no idioma prioritário: ${languageStr}.

                    ${docTextHeader}

                    ${styleInstructions}

                    Regras Importantes:
                    1. Mantenha a resposta com tamanho moderado, ideal para ser lida e de fácil assimilação em um chat dinâmico.
                    2. Use emojis amigáveis e formatações ricas em negrito para facilitar a leitura.
                    3. Responda de forma conectada e de apoio (empatia máxima).
                """.trimIndent()

                val apiContents = history.map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.message))
                    )
                } + Content(role = "user", parts = listOf(Part(text = userMessage)))

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                val responseText = try {
                    val response = RetrofitClient.service.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        request = request
                    )
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Desculpe, tive um contratempo para acessar minha base de dados agora. Vamos tentar de novo?"
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Erro ao conectar com o tutor do SinapseMed: ${e.localizedMessage}"
                }

                // Adiciona a resposta do Tutor ao chat
                val tutorChatMsg = ChatMessage(
                    id = 0,
                    documentId = currentDoc?.id ?: 0,
                    role = "model",
                    message = responseText,
                    timestamp = System.currentTimeMillis()
                )
                _tutorMessages.value = _tutorMessages.value + tutorChatMsg

                // Aumenta nível de empatia/conexão gradualmente
                val currentEmpathy = _tutorEmpathyLevel.value
                if (currentEmpathy < 100) {
                    _tutorEmpathyLevel.value = minOf(100, currentEmpathy + 12)
                }

                _tutorState.value = "SPEAKING"
                onResponseReady(responseText)
            } catch (e: Exception) {
                e.printStackTrace()
                _tutorState.value = "IDLE"
            }
        }
    }


    // --- ESTADOS DO ÁUDIO (TEXT TO SPEECH) ---
    private var tts: TextToSpeech? = null
    
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsReady.value = true
                setTtsLanguage()
            }
        }
    }

    private fun setTtsLanguage() {
        val locale = when (_appLanguage.value) {
            AppLanguage.PT -> Locale("pt", "BR")
            AppLanguage.EN -> Locale.US
            AppLanguage.ES -> Locale("es", "ES")
        }
        tts?.language = locale
    }

    fun speakAudioSummary(textToSpeak: String) {
        initTts() // Garante inicialização
        viewModelScope.launch(Dispatchers.Main) {
            val cleanedText = textToSpeak.replace(Regex("[*#_`~]"), "") // Remove markdown
            setTtsLanguage()
            tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, "SummaryUtterance")
            _isTtsSpeaking.value = true
        }
    }

    fun stopAudioSummary() {
        tts?.stop()
        _isTtsSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }

    // Inicialização do PDFBox
    init {
        viewModelScope.launch(Dispatchers.IO) {
            PdfHelper.init(application)
        }
    }

    /**
     * Importa, analisa, gera resumo enriquecido e salva o arquivo (PDF, Áudio, Vídeo ou Imagem) no banco de dados local.
     */
    fun importFile(uri: Uri, fileName: String, fileType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null
            _loadingStatusText.value = when (_appLanguage.value) {
                AppLanguage.PT -> "Processando arquivo $fileType..."
                AppLanguage.EN -> "Processing $fileType file..."
                AppLanguage.ES -> "Procesando archivo $fileType..."
            }

            try {
                // 1. Extração de texto dependendo do tipo do arquivo
                _loadingStatusText.value = when (_appLanguage.value) {
                    AppLanguage.PT -> "Processando conteúdo e realizando leitura médica (IA)..."
                    AppLanguage.EN -> "Reading clinical content (AI)..."
                    AppLanguage.ES -> "Extrayendo contenido clínico (IA)..."
                }
                
                val text = extractTextFromMedia(uri, fileType)
                if (text.isBlank() && fileType == "PDF") {
                    throw Exception("Erro ao ler conteúdo do documento.")
                }

                _loadingStatusText.value = when (_appLanguage.value) {
                    AppLanguage.PT -> "Identificando matéria, assunto e completando com fontes científicas clássicas..."
                    AppLanguage.EN -> "Structuring medical subject, topic, and traditional textbook sources..."
                    AppLanguage.ES -> "Identificando asignatura, asunto y completando con fuentes clínicas..."
                }

                // 2. Produzir o resumo contendo classificação de Matéria, Assunto, Fontes Enriquecidas e Resumo Clínico
                val rawResponse = generateEnrichedSummary(fileName, text, fileType)

                // Extrair as tags do resultado
                val subject = if (rawResponse.contains("[SUBJECT]")) {
                    rawResponse.substringAfter("[SUBJECT]").substringBefore("[TOPIC]").trim()
                } else "Medicina"

                val topic = if (rawResponse.contains("[TOPIC]")) {
                    rawResponse.substringAfter("[TOPIC]").substringBefore("[ENRICHED_SOURCES]").trim()
                } else "Estudos Clínicos"

                val enrichedSources = if (rawResponse.contains("[ENRICHED_SOURCES]")) {
                    rawResponse.substringAfter("[ENRICHED_SOURCES]").substringBefore("[SUMMARY]").trim()
                } else "Consulte os livros universitários tradicionais da matéria."

                val summaryText = if (rawResponse.contains("[SUMMARY]")) {
                    rawResponse.substringAfter("[SUMMARY]").trim()
                } else rawResponse

                val pageCount = if (fileType == "PDF") PdfHelper.getPageCount(getApplication(), uri) else 1

                _loadingStatusText.value = when (_appLanguage.value) {
                    AppLanguage.PT -> "Gravando progresso médico no banco de dados local..."
                    AppLanguage.EN -> "Saving student progress to local clinical storage..."
                    AppLanguage.ES -> "Guardando en el registro médico local..."
                }

                val savedDoc = SavedDocument(
                    fileName = fileName,
                    uriString = uri.toString(),
                    pageCount = pageCount,
                    extractedText = text,
                    summary = summaryText,
                    fileType = fileType,
                    subject = subject,
                    topic = topic,
                    enrichedSources = enrichedSources,
                    userLvlProgress = 0 // Estágio inicial de sondagem
                )

                val docId = repository.insertDocument(savedDoc)
                val finalDoc = savedDoc.copy(id = docId)

                // Selecionar documento recém-adicionado
                _selectedDocument.value = finalDoc

                if (fileType == "PDF") {
                    renderPagesAsync(uri, pageCount)
                }

                // Inserir mensagem automática inicial do modelo como o resumo completado
                val initialChatMessage = """
                    🩺 **Estudo de Caso Carregado**
                    
                    📖 **Matéria**: $subject
                    📌 **Assunto**: $topic
                    
                    ---
                    
                    📚 **Fontes e Livros Adicionais Recomendados**:
                    $enrichedSources
                    
                    ---
                    
                    📝 **Resumo do Conteúdo**:
                    $summaryText
                """.trimIndent()

                repository.insertChatMessage(
                    ChatMessage(
                        documentId = docId,
                        role = "model",
                        message = initialChatMessage
                    )
                )

                // Limpar dados do quiz anterior e apresentações
                _quizQuestions.value = emptyList()
                _videoScript.value = ""
                _slides.value = emptyList()

                // Auto gerar apresentação de slides e o quiz inicial
                generateSlidesForSelectedDocument()
                generateQuizForSelectedDocument()

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Erro: ${e.localizedMessage ?: "Exception"}"
            } finally {
                _isLoading.value = false
                _loadingStatusText.value = ""
            }
        }
    }

    private suspend fun extractTextFromMedia(uri: Uri, fileType: String): String {
        if (!isApiKeyConfigured) return "Chave da API do Gemini não configurada em AI Studio Secrets."
        
        return try {
            when (fileType) {
                "PDF" -> {
                    PdfHelper.extractText(getApplication(), uri)
                }
                "PHOTO" -> {
                    val pair = PdfHelper.getScaledImageBytes(getApplication(), uri) ?: return "Não foi possível processar a imagem do prontuário."
                    val base64Data = android.util.Base64.encodeToString(pair.first, android.util.Base64.NO_WRAP)
                    
                    val systemInstruction = "Você é um assistente médico perito em OCR e digitalização de anotações universitárias e slides de aula de medicina."
                    val prompt = "Transcreva de forma idêntica e complete com as devidas explicações todos os apontamentos, textos manuscritos, desenhos anatômicos ou resumos contidos nesta imagem em português."
                    
                    val inlineData = InlineData(mimeType = pair.second, data = base64Data)
                    val contents = listOf(
                        Content(parts = listOf(Part(text = prompt), Part(inlineData = inlineData)), role = "user")
                    )
                    val request = GenerateContentRequest(
                        contents = contents,
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        request = request
                    )
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Texto médico não legível na imagem."
                }
                "AUDIO" -> {
                    val pair = PdfHelper.getUriBytesAndMime(getApplication(), uri) ?: return "Não foi possível obter o arquivo de som original."
                    val base64Data = android.util.Base64.encodeToString(pair.first, android.util.Base64.NO_WRAP)
                    
                    val systemInstruction = "Você é um assistente médico especialista em receber áudios gravados de teleaulas de medicina e produzir notas didáticas."
                    val prompt = "Preste muita atenção ao áudio médico do aluno e transcreva e resuma de forma exaustiva e elegante todas as explicações verbais, raciocínios fisiopatológicos, drogas e tratamentos discutidos."
                    
                    val inlineData = InlineData(mimeType = pair.second, data = base64Data)
                    val contents = listOf(
                        Content(parts = listOf(Part(text = prompt), Part(inlineData = inlineData)), role = "user")
                    )
                    val request = GenerateContentRequest(
                        contents = contents,
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        request = request
                    )
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Áudio inaudível ou sem conteúdo de voz."
                }
                "VIDEO" -> {
                    val pair = PdfHelper.getUriBytesAndMime(getApplication(), uri) ?: return "Não foi possível carregar o conteúdo de vídeo."
                    
                    // Cap de segurança de dados para requests restritos
                    val bytes = if (pair.first.size > 14 * 1024 * 1024) {
                        pair.first.copyOfRange(0, 14 * 1024 * 1024)
                    } else pair.first
                    val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    
                    val systemInstruction = "Você é o mestre clínico do SinapseMed, analista avançado de vídeo-aulas práticas e cenários de simulação médica virtual."
                    val prompt = "Assista a este vídeo didático do aluno. Redija uma nota acadêmica completa contendo tudo o que é falado na gravação e todo o material visual, casos expostos ou esquemas mostrados na tela detalhadamente."
                    
                    val inlineData = InlineData(mimeType = pair.second, data = base64Data)
                    val contents = listOf(
                        Content(parts = listOf(Part(text = prompt), Part(inlineData = inlineData)), role = "user")
                    )
                    val request = GenerateContentRequest(
                        contents = contents,
                        systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        request = request
                    )
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Vídeo sem áudio descritivo discernível."
                }
                else -> ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Falha ao realizar a leitura inteligente do arquivo pela IA do SinapseMed."
        }
    }

    /**
     * Importa, analisa, gera resumo e salva o PDF no banco de dados local.
     */
    fun importPdf(uri: Uri, fileName: String) {
        importFile(uri, fileName, "PDF")
    }

    /**
     * Seleciona e carrega o histórico e páginas de um documento já existente.
     */
    fun selectDocument(document: SavedDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedDocument.value = document
            _selectedDocumentPages.value = emptyList() // limpa o cache anterior
            restartQuiz()

            // Carrega slides se presentes no banco de dados
            if (document.videoPresentationJson.isNotEmpty()) {
                _slides.value = parseSlides(document.videoPresentationJson)
            } else {
                _slides.value = emptyList()
            }

            // Carrega o quiz se presente no banco de dados
            if (document.quizJson.isNotEmpty()) {
                _quizQuestions.value = parseQuizOutput(document.quizJson)
            } else {
                _quizQuestions.value = emptyList()
            }

            _videoScript.value = document.audioScript

            try {
                if (document.fileType == "PDF") {
                    val uri = Uri.parse(document.uriString)
                    renderPagesAsync(uri, document.pageCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Exclui um documento do histórico.
     */
    fun deleteDocument(documentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedDocument.value?.id == documentId) {
                _selectedDocument.value = null
                _selectedDocumentPages.value = emptyList()
                _quizQuestions.value = emptyList()
                _videoScript.value = ""
            }
            repository.deleteDocument(documentId)
        }
    }

    /**
     * Envia uma pergunta ao cérebro de inteligência artificial sobre o PDF selecionado.
     */
    fun sendMessage(userText: String) {
        val doc = _selectedDocument.value ?: return
        if (userText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _loadingStatusText.value = when (_appLanguage.value) {
                AppLanguage.PT -> "A IA está processando..."
                AppLanguage.EN -> "AI is processing..."
                AppLanguage.ES -> "La IA está procesando..."
            }

            try {
                // 1. Salva a mensagem do usuário no chat
                val userMsg = ChatMessage(
                    documentId = doc.id,
                    role = "user",
                    message = userText
                )
                repository.insertChatMessage(userMsg)

                // 2. Coleta mensagens anteriores do chat para repassar como histórico ao Gemini
                val messagesHistory = chatMessages.value.toMutableList()
                if (!messagesHistory.any { it.message == userText }) {
                    messagesHistory.add(userMsg)
                }

                // Configura o prompt com foco contextual no PDF
                val responseText = queryGeminiWithDocumentContext(doc, messagesHistory)

                // 3. Salva a resposta do modelo no chat
                val modelMsg = ChatMessage(
                    documentId = doc.id,
                    role = "model",
                    message = responseText
                )
                repository.insertChatMessage(modelMsg)

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Erro: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _loadingStatusText.value = ""
            }
        }
    }

    /**
     * Limpa o chat em andamento do documento selecionado.
     */
    fun clearChatHistory() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChat(doc.id)
            // Re-insere o resumo inicial do documento para não ficar vazio
            repository.insertChatMessage(
                ChatMessage(
                    documentId = doc.id,
                    role = "model",
                    message = doc.summary
                )
            )
        }
    }

    /**
     * Desmarca o documento selecionado para retornar ao menu inicial.
     */
    fun deselectDocument() {
        _selectedDocument.value = null
        _selectedDocumentPages.value = emptyList()
        _quizQuestions.value = emptyList()
        _videoScript.value = ""
    }

    // --- MANIPULADORES DO QUIZ INTERATIVO ---

    fun selectAnswer(answerIndex: Int) {
        if (_selectedAnswerIdx.value != null) return // já respondeu a esta questão
        _selectedAnswerIdx.value = answerIndex
        _showExplanation.value = true
        val currentQuestion = _quizQuestions.value.getOrNull(_activeQuestionIdx.value)
        if (currentQuestion != null && currentQuestion.correctAnswerIdx == answerIndex) {
            _quizScore.value += 1
        }
    }

    fun nextQuizQuestion() {
        val nextIdx = _activeQuestionIdx.value + 1
        if (nextIdx < _quizQuestions.value.size) {
            _activeQuestionIdx.value = nextIdx
            _selectedAnswerIdx.value = null
            _showExplanation.value = false
        }
    }

    fun restartQuiz() {
        _activeQuestionIdx.value = 0
        _selectedAnswerIdx.value = null
        _showExplanation.value = false
        _quizScore.value = 0
    }

    /**
     * Gera perguntas baseadas no nível de estudo corrente ou na sondagem inicial.
     */
    fun generateQuizForSelectedDocument() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _quizLoading.value = true
            try {
                _quizQuestions.value = emptyList()
                restartQuiz()

                val currentLvl = doc.userLvlProgress
                val isSondagem = currentLvl == 0

                val languageStr = when (_appLanguage.value) {
                    AppLanguage.PT -> "Português"
                    AppLanguage.EN -> "English"
                    AppLanguage.ES -> "Español"
                }

                val systemInstruction = """
                    Você é a inteligência de avaliação médica do SinapseMed. Sua missão é criar questionários de estudos para verificar os conhecimentos médicos dos alunos com base no assunto.
                    Idioma: $languageStr.
                """.trimIndent()

                val prompt = if (isSondagem) {
                    """
                        O aluno deseja realizar a "Sondagem Inicial de Conhecimento" para aferir seu aprendizado sobre o assunto de estudos.
                        Gere exatamente 3 perguntas de múltipla escolha:
                        - Pergunta 1: Nível Fácil (anatomia básica ou conceitos essenciais).
                        - Pergunta 2: Nível Médio (mecanismos fisiológicos ou efeitos de medicações).
                        - Pergunta 3: Nível Difícil (caso de diagnóstico diferencial complexo típico de provas de residência médica).
                        
                        Texto base:
                        ---
                        ${doc.extractedText.take(40000)}
                        ---
                        
                        Use rigorosamente este formato de tags para que possamos ler cada bloco de forma algorítmica:
                        Q_START
                        [DIFFICULTY] Sondagem
                        [QUESTION] Pergunta da Sondagem?
                        [O1] Primeira opção
                        [O2] Segunda opção
                        [O3] Terceira opção
                        [O4] Quarta opção
                        [CORRECT] 0
                        [EXPLANATION] Explicação didática apontando o diagnóstico ou justificativa da resposta.
                        Q_END
                        
                        Crie exatamente as 3 perguntas estruturadas.
                    """.trimIndent()
                } else {
                    val levelDetails = when (currentLvl) {
                        1 -> "Nível 1 (Básico: Aspectos Fisiológicos e Conceituais). Questões conceituais, terminologia médica básica e anatomia funcional básica."
                        2 -> "Nível 2 (Intermediário: Fisiopatologia e Farmacologia Básica). Questões de mecanismos fisiopatológicos, alterações laboratoriais e efeito terapêutico de medicamentos clássicos."
                        3 -> "Nível 3 (Avançado: Casos Clínicos Complexos de Residência e Terapia Prática). Questões estilo provas seletivas de residência médica de cenários de emergência e diagnóstico preciso."
                        else -> "Geral"
                    }
                    """
                        O aluno está na fase de estudos: $levelDetails.
                        Gere exatamente 5 perguntas de múltipla escolha focadas estritamente nesta fase ($levelDetails) para sabermos se ele assimilou e consolidou as informações para que possa passar de fase.
                        
                        Texto base:
                        ---
                        ${doc.extractedText.take(40000)}
                        ---
                        
                        Use rigorosamente este formato de tags para que possamos ler cada bloco de forma algorítmica:
                        Q_START
                        [DIFFICULTY] Nível $currentLvl
                        [QUESTION] Pergunta do Quiz?
                        [O1] Primeira opção
                        [O2] Segunda opção
                        [O3] Terceira opção
                        [O4] Quarta opção
                        [CORRECT] 0
                        [EXPLANATION] Explicação médica preceituada detalhada ensinando o raciocínio clínico correto.
                        Q_END
                        
                        Crie exatamente as 5 perguntas estruturadas.
                    """.trimIndent()
                }

                val response = callGeminiApi(systemInstruction, prompt, emptyList())
                val parsedList = parseQuizOutput(response)
                
                _quizQuestions.value = parsedList
                
                // Salvar cache no banco local
                val updatedDoc = doc.copy(quizJson = response)
                repository.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _quizLoading.value = false
            }
        }
    }

    private fun parseQuizOutput(text: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        val blocks = text.split("Q_START")
        var count = 1

        for (block in blocks) {
            if (!block.contains("Q_END")) continue
            try {
                val difficulty = block.substringAfter("[DIFFICULTY]").substringBefore("\n").trim()
                val question = block.substringAfter("[QUESTION]").substringBefore("\n").trim()
                val o1 = block.substringAfter("[O1]").substringBefore("\n").trim()
                val o2 = block.substringAfter("[O2]").substringBefore("\n").trim()
                val o3 = block.substringAfter("[O3]").substringBefore("\n").trim()
                val o4 = block.substringAfter("[O4]").substringBefore("\n").trim()
                val correctStr = block.substringAfter("[CORRECT]").substringBefore("\n").trim()
                val explanation = block.substringAfter("[EXPLANATION]").substringBefore("Q_END").trim()

                val correctIdx = correctStr.toIntOrNull() ?: 0

                list.add(
                    QuizQuestion(
                        index = count++,
                        question = question,
                        options = listOf(o1, o2, o3, o4),
                        correctAnswerIdx = correctIdx,
                        difficulty = difficulty,
                        explanation = explanation
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    /**
     * Completa o questionário e avalia o progresso gradual do estudante de medicina.
     */
    fun completeQuizAndEvaluateProgress(onEvaluated: (Int) -> Unit) {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val score = _quizScore.value
            val total = _quizQuestions.value.size
            if (total == 0) return@launch
            
            val isSondagem = doc.userLvlProgress == 0
            val nextLvl = if (isSondagem) {
                when {
                    score >= 3 -> 3
                    score == 2 -> 2
                    else -> 1
                }
            } else {
                val percent = (score.toFloat() / total.toFloat()) * 100
                if (percent >= 80.0f) {
                    Math.min(doc.userLvlProgress + 1, 4) // Max 4 (Completo/Masterizado)
                } else {
                    doc.userLvlProgress // não mudou
                }
            }
            
            // Grava no banco de dados local
            val updatedDoc = doc.copy(userLvlProgress = nextLvl)
            repository.updateDocument(updatedDoc)
            _selectedDocument.value = updatedDoc
            
            withContext(Dispatchers.Main) {
                onEvaluated(nextLvl)
            }
            
            // Se o nível mudou ou completou a sondagem, regenera roteiros e slides para o novo nível acadêmico!
            generateSlidesForSelectedDocument()
            generateVideoScriptForSelectedDocument()
        }
    }

    /**
     * Permite ao estudante saltar entre níveis para explorar livremente.
     */
    fun setManuallyLevel(level: Int) {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedDoc = doc.copy(userLvlProgress = level)
            repository.updateDocument(updatedDoc)
            _selectedDocument.value = updatedDoc
            
            generateSlidesForSelectedDocument()
            generateQuizForSelectedDocument()
            generateVideoScriptForSelectedDocument()
        }
    }

    /**
     * Desenvolve os slides conforme a fase acadêmica atual do estudante.
     */
    fun generateSlidesForSelectedDocument() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _slidesLoading.value = true
            try {
                val currentLvl = if (doc.userLvlProgress <= 0) 1 else doc.userLvlProgress
                val levelDescription = when (currentLvl) {
                    1 -> "Nível 1 (Básico): Anatomia descritiva de base, Definições Gerais e Léxico Médico"
                    2 -> "Nível 2 (Intermediário): Raciocínio Clínico Fisiopatológico, Bioquímica de Patologia e Farmacologia das ações"
                    3 -> "Nível 3 (Avançado): Emergências médicas capitais, Raciocínio Sob Pressão e Diagnóstico Clínico"
                    else -> "Conceitos Clínicos"
                }

                val languageStr = when (_appLanguage.value) {
                    AppLanguage.PT -> "Português"
                    AppLanguage.EN -> "English"
                    AppLanguage.ES -> "Español"
                }

                val systemInstruction = """
                    Você é a IA do SinapseMed Visual Slides, especialista de ponta em fazer apresentações de slides médicos sucintos, visuais e explicativos de teleaulas.
                    Sua missão é desenvolver o material visual de slides médico de apoio para o nível de estudos atual do aluno: $levelDescription.
                    Idioma: $languageStr.
                """.trimIndent()

                val prompt = """
                    Com base no conteúdo de estudos fornecido abaixo, elabore exatamente 4 slides didáticos e elegantes direcionados no nível de estudos do aluno ($levelDescription).
                    Fomente o aprendizado gradual para que o conteúdo seja perfeitamente integrado.
                    
                    Use com absoluto rigor este formato de tags para cada slide:
                    
                    SLIDE_START
                    [TITLE] Título conciso do slide de medicina
                    [CONTENT]
                    - Tópico clínico de fisiologia primordial...
                    - Detalhes terapêuticos e farmacológicos chaves...
                    - Correlação com a prática real ou exames...
                    [VISUAL_PROMPT] Descrição clínica sucinta da ilustração anatômica ou esquema ideal de apoio para este slide.
                    SLIDE_END

                    Sua resposta deve conter exatamente 4 slides de medicina.

                    Texto de estudos:
                    ---
                    ${doc.extractedText.take(45000)}
                    ---
                """.trimIndent()

                val response = callGeminiApi(systemInstruction, prompt, emptyList())
                val parsedSlides = parseSlides(response)
                
                _slides.value = parsedSlides
                _activeSlideIdx.value = 0
                
                val updatedDoc = doc.copy(videoPresentationJson = response)
                repository.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _slidesLoading.value = false
            }
        }
    }

    fun parseSlides(text: String): List<SlidePage> {
        val list = mutableListOf<SlidePage>()
        val blocks = text.split("SLIDE_START")
        for (block in blocks) {
            if (!block.contains("SLIDE_END")) continue
            try {
                val title = block.substringAfter("[TITLE]").substringBefore("\n").trim()
                val content = block.substringAfter("[CONTENT]").substringBefore("[VISUAL_PROMPT]").trim()
                val visualPrompt = block.substringAfter("[VISUAL_PROMPT]").substringBefore("SLIDE_END").trim()
                
                list.add(SlidePage(title, content, visualPrompt))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    /**
     * Roteiriza a teleaula com base no nível atual acadêmico do estudante.
     */
    fun generateVideoScriptForSelectedDocument() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _videoLoading.value = true
            try {
                _videoScript.value = ""
                val currentLvl = if (doc.userLvlProgress <= 0) 1 else doc.userLvlProgress
                val levelDescription = when (currentLvl) {
                    1 -> "Nível Fácil 1 (Terminologia Geral, Anatomia descritiva de base e Aspectos fundamentais)"
                    2 -> "Nível Médio 2 (Fisiopatologia de base, Bioquímica e Tratamento farmacológico principal)"
                    3 -> "Nível Residência Avançada 3 (Emergências, Condutas urgentes e Diagnósticos diferenciais complexos)"
                    else -> "Nível Clínico Profissional"
                }

                val languageStr = when (_appLanguage.value) {
                    AppLanguage.PT -> "Português"
                    AppLanguage.EN -> "English"
                    AppLanguage.ES -> "Español"
                }

                val systemInstruction = """
                    Você é o renomado Preceptor Médico do SinapseMed, mestre e reitor universitário especialista em teleaulas médicas de altíssimo gabarito.
                    Sua missão é desenvolver o roteiro falado de teleaula clínica direcionada exclusivamente no nível curricular atual do aluno: $levelDescription.
                    Idioma: $languageStr.
                """.trimIndent()

                val prompt = """
                    Com base no material enviado, redija um Roteiro de Teleaula Médica emocionante que use indicações visuais entre colchetes (ex: [Cena: Gráfico de saturação e volemia é exposto na tela]) coordenadas de perto com as falas do preceptor clínico (começadas por "Preceptor: ") de forma exemplar focada em ensinar o aluno no nível: $levelDescription.

                    Seções requeridas:
                    1. 🩺 🎬 **Sessão Conhecimento de Base & Introdução**: Visão geral e analogias clínicas de fácil memorização.
                    2. 🧬 📈 **Análise Celular & Fisiopatologia**: O reponsável pela doença no organismo.
                    3. 💊 🧠 **Condutas Clínicas e Escolhas Farmacológicas Práticas**: O que receitar e como acompanhar o caso.
                    4. 🏥 💡 **Pegadinha da Residência**: Erros comuns que derrubam alunos e a explicação teórica corretiva.

                    Material base:
                    ---
                    ${doc.extractedText.take(45000)}
                    ---
                """.trimIndent()

                val response = callGeminiApi(systemInstruction, prompt, emptyList())
                _videoScript.value = response
                
                val updatedDoc = doc.copy(audioScript = response)
                repository.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
                
            } catch (e: Exception) {
                e.printStackTrace()
                _videoScript.value = "Erro: ${e.localizedMessage}"
            } finally {
                _videoLoading.value = false
            }
        }
    }

    /**
     * Produz o resumo completo contendo o enriquecimento científico com os livros base clássicos da universidade.
     */
    private suspend fun generateEnrichedSummary(fileName: String, text: String, fileType: String): String {
        if (!isApiKeyConfigured) {
            return "[SUBJECT] Medicina\n[TOPIC] Clínico Geral\n[ENRICHED_SOURCES]\nChave Gemini API ausente no painel de segredos do AI Studio.\n[SUMMARY]\nChave ausente."
        }

        val languageStr = when (_appLanguage.value) {
            AppLanguage.PT -> "Português"
            AppLanguage.EN -> "English"
            AppLanguage.ES -> "Español"
        }

        val systemInstruction = """
            Você é o SinapseMed Cérebro Acadêmico, curador de excelência bibliográfica médica e diretrizes de saúde internacionais.
            Sua missão é ler o arquivo de estudos do aluno (formato: $fileType, arquivo: '$fileName') e:
            1. Identificar com precisão qual a matéria médica universitária (ex: Cardiologia, Fisiologia Humana, Farmacologia, Patologia, Pediatria, Ginecologia, Nefrologia, Hematologia, etc.).
            2. Identificar qual o assunto médico exato estudado.
            3. Enriquecer e estender o material com os livros e referências bibliográficas universitárias tradicionais (títulos reais como 'Guyton & Hall Fisiologia Médica', 'Robbins & Cotran Patologia Básica', 'Harrison Princípios de Medicina Interna', etc.) e dados de diretrizes de saúde adicionais da internet pertinentes.
        """.trimIndent()

        val prompt = """
            Analise o conteúdo acadêmico extraído e responda no idioma $languageStr estruturado RIGOROSAMENTE com as tags abaixo para que possamos extrair as partes via algoritmo:

            [SUBJECT] Matéria universitária (substitua pelo nome da matéria identificada, ex: Cardiologia)
            [TOPIC] Assunto médico específico (ex: Insuficiência Cardíaca Congestiva)
            [ENRICHED_SOURCES]
            Aqui traga as referências dos livros universitários tradicionais (com títulos de livros reais que os reitores cobram nas faculdades, como Guyton, Robbins, Harrison) e traga dados adicionais vitais de consensos médicos e diretrizes de saúde atualizados da internet, completando o que falta no material do aluno de forma impecável. Use listas formatadas e explicações curtas e ricas. Only medical/clinical textbook sources.
            [SUMMARY]
            Escreva o Resumo Clínico completo e didático do documento no idioma $languageStr, dividido detalhadamente em:
            - 📌 **Visão Geral**
            - 🔑 **Pontos Fisiopatológicos e Clínicos Principais**
            - 💡 **Abordagem de Conduta e Diagnóstico Recomendado**

            Conteúdo de estudos do aluno:
            ---
            ${text.take(65000)}
            ---
        """.trimIndent()

        return callGeminiApi(systemInstruction, prompt, emptyList())
    }

    fun toggleVideoPlayback() {
        _isVideoPlaying.value = !_isVideoPlaying.value
    }

    private suspend fun generatePdfSummary(fileName: String, text: String, pageCount: Int): String {
        return generateEnrichedSummary(fileName, text, "PDF")
    }

    // --- CHAT COM CONTEXTO ---

    private suspend fun queryGeminiWithDocumentContext(
        doc: SavedDocument,
        history: List<ChatMessage>
    ): String {
        if (!isApiKeyConfigured) {
            return "API Key error."
        }

        val languageStr = when (_appLanguage.value) {
            AppLanguage.PT -> "Português"
            AppLanguage.EN -> "English"
            AppLanguage.ES -> "Español"
        }

        val systemInstruction = """
            Você é o PDF Brain, uma IA conectada de verdade ao PDF '${doc.fileName}'.
            Seu idioma prioritário de resposta deve ser: $languageStr.
            Aqui está todo o texto extraído do PDF:
            ---
            ${doc.extractedText.take(120000)}
            ---
            Instruções:
            1. Responda fundamentado no conteúdo extraído do PDF sempre que possível.
            2. Se a resposta não estiver no PDF, responda cordialmente apontando isso, fornecendo conhecimentos gerais sobre o assunto de forma estruturada e profissional.
            3. Use listas, negritos e excelente espaçamento.
        """.trimIndent()

        val apiContents = history.map { msg ->
            Content(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(Part(text = msg.message))
            )
        }

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Desculpe, não consegui formular uma resposta a partir deste documento."
        } catch (e: Exception) {
            e.printStackTrace()
            "Erro: ${e.localizedMessage}"
        }
    }

    private suspend fun callGeminiApi(
        systemInstruction: String,
        prompt: String,
        history: List<Content>
    ): String {
        val contents = history + listOf(Content(parts = listOf(Part(text = prompt)), role = "user"))
        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val response = RetrofitClient.service.generateContent(
            model = "gemini-3.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "Response empty."
    }

    private suspend fun renderPagesAsync(uri: Uri, pageCount: Int) {
        withContext(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            val maxPagesToRender = minOf(pageCount, 15)

            for (i in 0 until maxPagesToRender) {
                PdfHelper.renderPageToBitmap(getApplication(), uri, i)?.let {
                    bitmaps.add(it)
                }
            }
            _selectedDocumentPages.value = bitmaps
        }
    }
}
