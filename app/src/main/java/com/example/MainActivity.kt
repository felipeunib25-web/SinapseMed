package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.ChatMessage
import com.example.data.db.SavedDocument
import com.example.ui.AppLanguage
import com.example.ui.PdfReaderViewModel
import com.example.ui.QuizQuestion
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Locale

// Gerenciador de Localização Rápida no Dispositivo
object Loc {
    fun get(key: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PT -> when (key) {
                "app_title" -> "SinapseMed"
                "subtitle" -> "Estudos de Medicina & Resumos de Aula Dirigidos por IA"
                "online" -> "TUTOR DE IA ONLINE"
                "offline" -> "TUTOR DE IA OFFLINE"
                "warning_title" -> "Aviso Importante"
                "warning_desc" -> "A chave da API do Gemini (IA) não está configurada! Por favor, acesse o painel Secrets (Segredos) e insira uma GEMINI_API_KEY válida para ativar a inteligência acadêmica e interagir com seus arquivos de aula de medicina e anotações."
                "upload_btn" -> "Carregar Material de Aula"
                "upload_desc" -> "Toque para selecionar PDFs de aulas universitárias, slides ou resumos teóricos"
                "recent_docs" -> "Arquivos de Aula e Slides Recentes"
                "empty_docs" -> "Nenhum material acadêmico lido"
                "empty_docs_desc" -> "Carregue anotações, slides ou PDFs de aula para iniciar o estudo assistido por IA!"
                "p_count_1" -> "página"
                "p_count_n" -> "páginas"
                "back" -> "Voltar"
                "clear_chat" -> "Reiniciar Discussão"
                "chat_title" -> "SESSÃO ACADÊMICA DE DISCUSSÃO COM O PDF:"
                "pages_title" -> "PÁGINAS REAIS DA AULA (TOQUE PARA VER):"
                "summary_title" -> "Resumo de Aula Fisiopatológica e Acadêmica"
                "ask_placeholder" -> "Pergunte sobre a matéria..."
                "thinking" -> "Analisando anatomia e dados acadêmicos..."
                "tts_play" -> "Ouvir Explicação (Áudio)"
                "tts_stop" -> "Pausar Áudio"
                "quiz_tab" -> "Simulado"
                "video_tab" -> "Teleaula / Slides"
                "chat_tab" -> "Tutor IA"
                "generate_quiz" -> "Desenvolver Simulado"
                "quiz_intro" -> "Gere um simulado acadêmico de questões personalizadas de diversos níveis teóricos e com casos clínicos integrados baseados nos dados desse material escolar."
                "difficulty_easy" -> "Fácil (Básico)"
                "difficulty_medium" -> "Médio (Fisiopatologia)"
                "difficulty_hard" -> "Difícil (Simulado de Prova)"
                "correct" -> "Correto!"
                "incorrect" -> "Incorreto! A justificativa acadêmica correta era:"
                "score" -> "Aproveitamento do Simulado:"
                "explanation_btn" -> "Ver Justificativa Baseada na Aula"
                "next_question" -> "Próxima Questão"
                "restart" -> "Refazer Simulado"
                "quiz_completed" -> "Simulado Acadêmico Concluído!"
                "video_intro" -> "Gere uma teleaula didática inteligente estruturada em módulos focados em fisiopatologia, diagnóstico e conduta teórica da matéria."
                "generate_script" -> "Desenvolver Teleaula"
                "script_title" -> "Análise e Roteiro de Aula"
                "play_video_lesson" -> "Reproduzir Teleaula"
                "pause_video_lesson" -> "Pausar Teleaula"
                "language_selection" -> "Idioma"
                "audio_lesson_title" -> "Teleaula SinapseMed"
                "audio_lesson_desc" -> "Discussão em áudio sobre o assunto de medicina"
                "chat_welcome_title" -> "IA Acadêmica Ativa!"
                "chat_welcome_desc" -> "Pronto para analisar condutas, diagnósticos e matérias do PDF! Sinta-se à vontade para tirar dúvidas teóricas."
                "page_count_label" -> "Pág"
                "text_to_speech" -> "Ouvir Teleaula"
                "stop" -> "Parar"
                "question_label" -> "Questão"
                "of" -> "de"
                "your_score" -> "Aproveitamento Final:"
                else -> key
            }
            AppLanguage.EN -> when (key) {
                "app_title" -> "SinapseMed"
                "subtitle" -> "Medical Mind: Lecture Summaries & AI Guided Academic Studies"
                "online" -> "ACADEMIC AI ONLINE"
                "offline" -> "ACADEMIC AI OFFLINE"
                "warning_title" -> "Important Warning"
                "warning_desc" -> "The Gemini API Key is not configured! Please open the Secrets tab or settings and insert a valid GEMINI_API_KEY to activate academic intelligence and study your documents & guidelines."
                "upload_btn" -> "Load Lecture PDF"
                "upload_desc" -> "Tap to select medical guidelines, papers, slides, or books"
                "recent_docs" -> "Recent Lectures & Study Sheets"
                "empty_docs" -> "No study materials analyzed"
                "empty_docs_desc" -> "Upload your first medical lecture or textbook chapter to get AI-guided insights!"
                "p_count_1" -> "page"
                "p_count_n" -> "pages"
                "back" -> "Back"
                "clear_chat" -> "Clear Sessions"
                "chat_title" -> "ACADEMIC CLASS DISCUSSION WITH PDF:"
                "pages_title" -> "REAL PAGES OF THE LECTURE (TAP TO VIEW):"
                "summary_title" -> "Scholarly & Pathology Summary"
                "ask_placeholder" -> "Ask about the subject..."
                "thinking" -> "Analyzing pathology and academic data..."
                "tts_play" -> "Listen to Preceptor (Audio)"
                "tts_stop" -> "Pause Audio"
                "quiz_tab" -> "Mock Exam"
                "video_tab" -> "Lecture / Slides"
                "chat_tab" -> "AI College Preceptor"
                "generate_quiz" -> "Develop Case Quiz"
                "quiz_intro" -> "Generate an interactive, customized academic quiz structured from basic concepts to advanced diagnostic questions based on your material."
                "difficulty_easy" -> "Easy (Concepts)"
                "difficulty_medium" -> "Medium (Diagnostic)"
                "difficulty_hard" -> "Hard (Exam Prep)"
                "correct" -> "Correct!"
                "incorrect" -> "Incorrect! The best scholarly approach / answer was:"
                "score" -> "Academic Performance:"
                "explanation_btn" -> "View Scholarly Reasoning"
                "next_question" -> "Next Question"
                "restart" -> "Retake Challenge"
                "quiz_completed" -> "Academic Quiz Completed!"
                "video_intro" -> "Generate a structured medical lecture focused on anatomy, disease pathophysiology, and theoretical slides."
                "generate_script" -> "Develop Lecture"
                "script_title" -> "Conceptual Outline & Script"
                "play_video_lesson" -> "Play Lecture"
                "pause_video_lesson" -> "Pause Lecture"
                "language_selection" -> "Language"
                "audio_lesson_title" -> "SinapseMed Lecture"
                "audio_lesson_desc" -> "Audio lecture simulation covering medical topics"
                "chat_welcome_title" -> "Academic AI Active!"
                "chat_welcome_desc" -> "Ready to review textbooks, lectures and notebook files! Ask any theoretical question."
                "page_count_label" -> "Pg"
                "text_to_speech" -> "Listen Lecture"
                "stop" -> "Stop"
                "question_label" -> "Question"
                "of" -> "of"
                "your_score" -> "Final Performance:"
                else -> key
            }
            AppLanguage.ES -> when (key) {
                "app_title" -> "SinapseMed"
                "subtitle" -> "Estudios Académicos y Teoría Médica con IA"
                "online" -> "PRECEPTOR ONLINE"
                "offline" -> "PRECEPTOR OFFLINE"
                "warning_title" -> "Aviso Importante"
                "warning_desc" -> "¡La clave API de Gemini (IA) no está configurada! Acceda al panel de Secretos e introduzca una GEMINI_API_KEY válida para activar la inteligencia académica y analizar sus guías teóricas."
                "upload_btn" -> "Cargar PDF Académico"
                "upload_desc" -> "Toque para seleccionar guías, diapositivas o apuntes de clase"
                "recent_docs" -> "Mis Materiales de Estudio"
                "empty_docs" -> "Ninguna clase analizada"
                "empty_docs_desc" -> "¡Cargue diapositivas o apuntes de medicina para iniciar el estudio asistido por IA!"
                "p_count_1" -> "página"
                "p_count_n" -> "páginas"
                "back" -> "Atrás"
                "clear_chat" -> "Limpiar Mentoría"
                "chat_title" -> "SESIÓN ACADÊMICA DE DISCUSIÓN CON EL PDF:"
                "pages_title" -> "PÁGINAS REAIS DE LA DIAPOSITIVA (TOCA PARA VER):"
                "summary_title" -> "Resumen de Clase Teórica y Fisiopatológica"
                "ask_placeholder" -> "Pregunte sobre la materia..."
                "thinking" -> "Analizando patología y bases teóricas..."
                "tts_play" -> "Escuchar Tutor Virtual (Audio)"
                "tts_stop" -> "Pausar Audio"
                "quiz_tab" -> "Similitud Examen"
                "video_tab" -> "Videoclase / Slides"
                "chat_tab" -> "Tutor IA"
                "generate_quiz" -> "Desarrollar Examen"
                "quiz_intro" -> "Genere un cuestionario académico personalizado estructurado de lo básico a lo experto basado en este material de clase."
                "difficulty_easy" -> "Fácil (Básico)"
                "difficulty_medium" -> "Medio (Intermedio)"
                "difficulty_hard" -> "Difícil (Residencia)"
                "correct" -> "¡Correcto!"
                "incorrect" -> "¡Incorrecto! La explicación académica correcta era:"
                "score" -> "Aprovechamiento Académico:"
                "explanation_btn" -> "Ver Justificación Teórica"
                "next_question" -> "Siguiente Pregunta"
                "restart" -> "Reiniciar Cuestionario"
                "quiz_completed" -> "¡Examen de Simulación Completado!"
                "video_intro" -> "Genere una videoclase educativa estruturada en módulos enfocados en anatomía, fisiopatología y abordaje clínico académico."
                "generate_script" -> "Desarrollar Videoclase"
                "script_title" -> "Análisis y Guión de Diapositivas"
                "play_video_lesson" -> "Reproducir Videoclase"
                "pause_video_lesson" -> "Pausar Videoclase"
                "language_selection" -> "Idioma"
                "audio_lesson_title" -> "Videoclase SinapseMed"
                "audio_lesson_desc" -> "Clase virtual en audio sobre el material de estudio"
                "chat_welcome_title" -> "¡IA Académica Activa!"
                "chat_welcome_desc" -> "¡Listo para analizar tratamientos, patologías y temas de clase del PDF! Sienta la libertad de hacer preguntas teóricas."
                "page_count_label" -> "Pág"
                "text_to_speech" -> "Escuchar Clase"
                "stop" -> "Parar"
                "question_label" -> "Pregunta"
                "of" -> "de"
                "your_score" -> "Aprovechamiento Final:"
                else -> key
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PdfReaderViewModel = viewModel()
            val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsState()
            MyApplicationTheme(darkTheme = darkThemeEnabled) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: PdfReaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe state from ViewModel
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    val docPages by viewModel.selectedDocumentPages.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusText by viewModel.loadingStatusText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var showUploadSelector by remember { mutableStateOf(false) }

    // Launcher para selecionar PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileName(context, uri)
            viewModel.importFile(uri, name, "PDF")
        }
    }

    // Launcher para selecionar Foto
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileName(context, uri)
            viewModel.importFile(uri, name, "PHOTO")
        }
    }

    // Launcher para selecionar Áudio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileName(context, uri)
            viewModel.importFile(uri, name, "AUDIO")
        }
    }

    // Launcher para selecionar Vídeo
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileName(context, uri)
            viewModel.importFile(uri, name, "VIDEO")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Soft ambient background image related to academic and clinical studies (lowered alpha to match light/dark themes perfectly)
        Image(
            painter = painterResource(id = R.drawable.academic_med_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.12f
        )

        Crossfade(
            targetState = selectedDoc,
            animationSpec = spring(),
            label = "ScreenTransition"
        ) { doc ->
            if (doc == null) {
                // TELA INICIAL (Lista e Upload)
                HomeDashboard(
                    documents = allDocs,
                    isLoading = isLoading,
                    isApiKeyConfigured = viewModel.isApiKeyConfigured,
                    onSelectDoc = { viewModel.selectDocument(it) },
                    onDeleteDoc = { viewModel.deleteDocument(it) },
                    onUploadClicked = { showUploadSelector = true },
                    lang = lang,
                    viewModel = viewModel
                )
            } else {
                // ÁREA DE WORKSPACE / CHAT DO DOCUMENTO
                DocumentWorkspace(
                    doc = doc,
                    pages = docPages,
                    messages = chatMessages,
                    isLoading = isLoading,
                    isApiKeyConfigured = viewModel.isApiKeyConfigured,
                    viewModel = viewModel,
                    lang = lang,
                    onChangeLanguage = { viewModel.changeLanguage(it) },
                    onBack = { 
                        keyboardController?.hide()
                        viewModel.deselectDocument() 
                    },
                    onSendMessage = { query ->
                        keyboardController?.hide()
                        viewModel.sendMessage(query)
                    },
                    onClearChat = {
                        viewModel.clearChatHistory()
                    }
                )
            }
        }

        // POPUP SELETOR DE ARQUIVOS ACADÊMICOS / MÉDICOS
        if (showUploadSelector) {
            AlertDialog(
                onDismissRequest = { showUploadSelector = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Medical upload",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) "Enviar Material de Estudos" else if (lang == AppLanguage.ES) "Enviar Material de Estudios" else "Upload Study Lectures",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PT) "Escolha o formato do seu material acadêmico para ativação da IA:" else "Select the format of your lecture/study file for AI reading:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // PDF Acadêmico
                        Card(
                            onClick = {
                                showUploadSelector = false
                                pdfPickerLauncher.launch("application/pdf")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(if (lang == AppLanguage.PT) "PDF (Aulas, Resumos, Livros, Artigos)" else "PDF (Lectures, Summaries, Textbooks)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(if (lang == AppLanguage.PT) "Leitura de material teórico completo de medicina" else "Full academic medical text reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // Foto de Caderno / Anatomia
                        Card(
                            onClick = {
                                showUploadSelector = false
                                photoPickerLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(if (lang == AppLanguage.PT) "Foto (Caderno, Slides do Professor, Resumos)" else "Photo (Notebook page, Slides, Summaries)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text(if (lang == AppLanguage.PT) "Transcrição inteligente de anotações e quadros" else "Smart transcription of student notes & boards", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // Áudio de Palestra / Teleaula
                        Card(
                            onClick = {
                                showUploadSelector = false
                                audioPickerLauncher.launch("audio/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(if (lang == AppLanguage.PT) "Áudio (Gravação de Aula, Explicações)" else "Audio (Class recording, Explanations)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(if (lang == AppLanguage.PT) "Transcrição inteligente de palestras e resumos de voz" else "Voice-activated lecture summarizer and speech notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // Vídeo Prático / Simulação
                        Card(
                            onClick = {
                                showUploadSelector = false
                                videoPickerLauncher.launch("video/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(if (lang == AppLanguage.PT) "Vídeo (Teleaula, Aula Expositiva Gravada)" else "Video (Telelecture, Recorded Class)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (lang == AppLanguage.PT) "Análise auditiva teórica e criação de resumos" else "Theoretical audio analysis and key study notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUploadSelector = false }) {
                        Text(if (lang == AppLanguage.PT) "Cancelar" else "Cancel")
                    }
                }
            )
        }

        // Global Loading overlay (when performing root level operations)
        if (isLoading && statusText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Global error snackbar/message box
        errorMessage?.let { err ->
            Snackbar(
                action = {
                    TextButton(onClick = { /* Could clear error or retry */ }) {
                        Text("OK", color = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(err)
            }
        }
    }
}

@Composable
fun HomeDashboard(
    documents: List<SavedDocument>,
    isLoading: Boolean,
    isApiKeyConfigured: Boolean,
    onSelectDoc: (SavedDocument) -> Unit,
    onDeleteDoc: (Int) -> Unit,
    onUploadClicked: () -> Unit,
    lang: AppLanguage,
    viewModel: PdfReaderViewModel
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            lang = lang,
            onDismiss = { showSettingsDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER COM TÍTULO E BOTÃO DE CONFIGURAÇÕES (SEM BANDEIRAS)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Loc.get("app_title", lang),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = Loc.get("subtitle", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // Ícone de Configurações para abrir o diálogo de preferências
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .testTag("app_settings_button")
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. STATUS BADGE E ALERTAS DE CONFIGURAÇÃO
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                StatusIndicatorBadge(isApiKeyConfigured = isApiKeyConfigured, lang = lang)
            }
        }

        if (!isApiKeyConfigured) {
            item {
                ApiKeyWarningCard(lang = lang)
            }
        }

        // 3. BOTÃO DE CARREGAMENTO MULTI-MÍDIA PREMIUM
        item {
            Card(
                onClick = onUploadClicked,
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .testTag("pdf_picker_button")
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Multi-format supports indicator icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                            Icon(imageVector = Icons.Default.AudioFile, contentDescription = "Audio", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "Video", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(26.dp))
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Image/Notes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) "Carregar Material de Estudos" else "Upload Study Material",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (lang == AppLanguage.PT) "Enviar Vídeos, Áudios, Cadernos de Notas ou PDFs" else "Supports Videos, Audio, Notebook Images, or PDFs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. AVISO EXPLICATIVO DE FORMATOS GERADOS PELO MATERIAL DE FONTE (MUITO ATRATIVO)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) "O que a IA SinapseDoc vai gerar:" else "What SinapseDoc generates for you:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.PT) "Após o carregamento, convertemos seu material nos seguintes formatos interativos:" 
                               else "Once loaded, we format your source files into custom interactive layouts:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val generatedFormats = listOf(
                        Triple(Icons.Default.ChromeReaderMode, if (lang == AppLanguage.PT) "Resumos Clínicos & Fisiopatológicos" else "Scholarly Concept Outlines", if (lang == AppLanguage.PT) "Síntese dos tópicos principais da matéria" else "Key textbook findings & diagnostics"),
                        Triple(Icons.Default.AudioFile, if (lang == AppLanguage.PT) "Podcast & Audioclases Explicativas" else "Explanatory Audiolectures", if (lang == AppLanguage.PT) "Discussão guiada em áudio" else "Walkthrough of concepts voiced out loud"),
                        Triple(Icons.Default.OndemandVideo, if (lang == AppLanguage.PT) "Videoaulas Interativas com Slides" else "Smart Visual tele-lecture slides", if (lang == AppLanguage.PT) "Visualização de slides e aula integrada" else "Structured video explanation modules"),
                        Triple(Icons.Default.Quiz, if (lang == AppLanguage.PT) "Simulados e Questões de Prova" else "Medical MCQ Quizzes & Core Cases", if (lang == AppLanguage.PT) "Casos de prova com feedback e justificativas" else "Immediate clinical feedback & analysis"),
                        Triple(Icons.Default.Science, if (lang == AppLanguage.PT) "Preceptor Virtual 3D Personalizado" else "3D Tutor Logical Adapters", if (lang == AppLanguage.PT) "Explicações sob medida ao seu perfil de raciocínio" else "Adapts content directly to your target learning style")
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        generatedFormats.forEach { (icon, title, desc) ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. LISTA DE DOCUMENTOS EXISTENTES
        item {
            Text(
                text = Loc.get("recent_docs", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (documents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty Folder",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = Loc.get("empty_docs", lang),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Loc.get("empty_docs_desc", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(documents, key = { it.id }) { doc ->
                DocumentListItem(
                    doc = doc,
                    onSelect = { onSelectDoc(doc) },
                    onDelete = { onDeleteDoc(doc.id) },
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun LanguageSelectorChip(
    activeLang: AppLanguage,
    targetLang: AppLanguage,
    flag: String,
    onClick: (AppLanguage) -> Unit
) {
    val isSelected = activeLang == targetLang
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .clickable { onClick(targetLang) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = flag,
            fontSize = 18.sp
        )
    }
}

@Composable
fun DocumentListItem(
    doc: SavedDocument,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    lang: AppLanguage
) {
    // Definir ícone, cor e nome amigável do formato
    val (icon, tint, formatBadge) = when (doc.fileType.uppercase()) {
        "PHOTO" -> Triple(Icons.Default.PhotoCamera, Color(0xFFE91E63), if (lang == AppLanguage.PT) "FOTO" else "PHOTO")
        "AUDIO" -> Triple(Icons.Default.Mic, Color(0xFF009688), if (lang == AppLanguage.PT) "ÁUDIO" else "AUDIO")
        "VIDEO" -> Triple(Icons.Default.PlayCircle, Color(0xFFFF9800), if (lang == AppLanguage.PT) "VÍDEO" else "VIDEO")
        else -> Triple(Icons.Default.Article, Color(0xFF2196F3), "PDF")
    }

    // Definir rótulo de progresso do nível do aluno
    val lvlBadgeText = when (doc.userLvlProgress) {
        0 -> if (lang == AppLanguage.PT) "Sondagem Inicial 🩺" else "Initial Assessment 🩺"
        1 -> if (lang == AppLanguage.PT) "Nível 1: Fundamentos 📚" else "Lvl 1: Basics 📚"
        2 -> if (lang == AppLanguage.PT) "Nível 2: Fisiopatologia 🔬" else "Lvl 2: Pathophysiology 🔬"
        3 -> if (lang == AppLanguage.PT) "Nível 3: Diagnóstico Clínico 🩺" else "Lvl 3: Clinical Diagnosis 🩺"
        4 -> if (lang == AppLanguage.PT) "Nível 4: Residência Médica 🏥" else "Lvl 4: Residency Practice 🏥"
        else -> if (lang == AppLanguage.PT) "Nível 5: Preceptor Especialista ⭐" else "Lvl 5: Specialist Master ⭐"
    }

    val lvlColor = when (doc.userLvlProgress) {
        0 -> Color(0xFF9C27B0)
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF03A9F4)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFFE91E63)
        else -> Color(0xFFFFD700)
    }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_document_card_${doc.id}")
            .border(
                1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emblema do Formato Clínico
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            tint.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Format icon",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge de formato do arquivo
                        Box(
                            modifier = Modifier
                                .background(tint.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = tint
                            )
                        }

                        // Badge de Nível de Aprendizado Atual
                        Box(
                            modifier = Modifier
                                .background(lvlColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                .border(1.dp, lvlColor.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = lvlBadgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (doc.userLvlProgress == 5) Color(0xFFB8860B) else lvlColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = doc.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .testTag("delete_document_button_${doc.id}")
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Document",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Seção de Matéria & Tópico Identificados de Forma Clínico e Científico
            if ((doc.subject ?: "").isNotEmpty() || (doc.topic ?: "").isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if ((doc.subject ?: "").isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = doc.subject,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if ((doc.topic ?: "").isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Topic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = doc.topic,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyWarningCard(lang: AppLanguage) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = Loc.get("warning_title", lang),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Loc.get("warning_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun StatusIndicatorBadge(isApiKeyConfigured: Boolean, lang: AppLanguage) {
    val bgColor = if (isApiKeyConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isApiKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val text = if (isApiKeyConfigured) Loc.get("online", lang) else Loc.get("offline", lang)

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isApiKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun DocumentWorkspace(
    doc: SavedDocument,
    pages: List<Bitmap>,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isApiKeyConfigured: Boolean,
    viewModel: PdfReaderViewModel,
    lang: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Chat & Resumo, 1 = Roteiro & Vídeo, 2 = Quiz Inteligente
    var zoomPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            lang = lang,
            onDismiss = { showSettingsDialog = false }
        )
    }

    val voiceGender by viewModel.tutorVoiceGender.collectAsState()
    val voiceSpeed by viewModel.tutorVoiceSpeed.collectAsState()

    // TTS Setup no Compose
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Configurou
            }
        }
        
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isTtsPlaying = true
            }
            override fun onDone(utteranceId: String?) {
                isTtsPlaying = false
            }
            override fun onError(utteranceId: String?) {
                isTtsPlaying = false
            }
        })

        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    LaunchedEffect(lang, tts, voiceGender, voiceSpeed) {
        val currentTts = tts ?: return@LaunchedEffect
        val locale = when (lang) {
            AppLanguage.PT -> Locale("pt", "BR")
            AppLanguage.EN -> Locale.US
            AppLanguage.ES -> Locale("es", "ES")
        }
        currentTts.language = locale
        val pitch = if (voiceGender == "male") 0.82f else 1.25f
        currentTts.setPitch(pitch)
        currentTts.setSpeechRate(voiceSpeed)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TopBar / Header do Documento
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    tts?.stop()
                    isTtsPlaying = false
                    onBack()
                },
                modifier = Modifier.testTag("back_to_list_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Loc.get("back", lang),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = doc.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${doc.pageCount} ${if (doc.pageCount == 1) Loc.get("p_count_1", lang) else Loc.get("p_count_n", lang)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Configurações do App (unificadas sem bandeiras na tela inicial)
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.testTag("workspace_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Loc.get("clear_chat", lang),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // TAB NAVIGATION ROW (Material 3)
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(Loc.get("chat_tab", lang), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(Loc.get("video_tab", lang), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(15.dp)) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(Loc.get("quiz_tab", lang), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(15.dp)) }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text(if (lang == AppLanguage.PT) "Professor 3D" else if (lang == AppLanguage.ES) "Tutor 3D" else "3D Mentor", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(15.dp)) }
            )
        }

        // CONTEÚDO DA ABA SELECIONADA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> {
                    // ABA 0: Chat e Resumo
                    ChatAndSummaryTab(
                        doc = doc,
                        pages = pages,
                        messages = messages,
                        isLoading = isLoading,
                        isApiKeyConfigured = isApiKeyConfigured,
                        lang = lang,
                        onSendMessage = onSendMessage,
                        onZoomPage = { zoomPageBitmap = it },
                        tts = tts,
                        isTtsPlaying = isTtsPlaying,
                        onToggleTts = {
                            if (isTtsPlaying) {
                                tts?.stop()
                                isTtsPlaying = false
                            } else {
                                isTtsPlaying = true
                                val params = Bundle()
                                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Summary")
                                tts?.speak(doc.summary, TextToSpeech.QUEUE_FLUSH, params, "Summary")
                            }
                        }
                    )
                }
                1 -> {
                    // ABA 1: Vídeo Aula / Roteiro Inteligente
                    VideoClassTab(
                        viewModel = viewModel,
                        lang = lang,
                        tts = tts
                    )
                }
                2 -> {
                    // ABA 2: Quiz Inteligente Gradual
                    SmartQuizTab(
                        viewModel = viewModel,
                        lang = lang
                    )
                }
                3 -> {
                    // ABA 3: Tutor 3D Interativo e Cognitivo
                    Tutor3DTab(
                        viewModel = viewModel,
                        lang = lang,
                        tts = tts
                    )
                }
            }
        }
    }

    // Modal de zoom para visualização em miniatura da página
    zoomPageBitmap?.let { bitmap ->
        Dialog(onDismissRequest = { zoomPageBitmap = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visualização da Página",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        IconButton(onClick = { zoomPageBitmap = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page zoom image view",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .border(1.dp, Color.LightGray),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun ChatAndSummaryTab(
    doc: SavedDocument,
    pages: List<Bitmap>,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isApiKeyConfigured: Boolean,
    lang: AppLanguage,
    onSendMessage: (String) -> Unit,
    onZoomPage: (Bitmap) -> Unit,
    tts: TextToSpeech?,
    isTtsPlaying: Boolean,
    onToggleTts: () -> Unit
) {
    val scrollState = rememberLazyListState()
    var inputQuery by remember { mutableStateOf("") }
    var isSummaryExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Seção 1: Páginas físicas lidas (Fotos/Previews) se houver
        if (pages.isNotEmpty()) {
            Text(
                text = Loc.get("pages_title", lang),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pages.size) { idx ->
                    Card(
                        modifier = Modifier
                            .width(76.dp)
                            .fillMaxHeight()
                            .clickable { onZoomPage(pages[idx]) },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = pages[idx].asImageBitmap(),
                                contentDescription = "Página ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(topStart = 6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Seção 2: Resumo Expandível + TTS integrado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSummaryExpanded = !isSummaryExpanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Resumo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Loc.get("summary_title", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ouvir Resumo em Áudio botão interativo
                        IconButton(
                            onClick = onToggleTts,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isTtsPlaying) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = "TTS",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isSummaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand toggle",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isSummaryExpanded) {
                    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        Text(
                            text = doc.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }

        // Linha de chat / diálogos
        Text(
            text = Loc.get("chat_title", lang),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() || (messages.size == 1 && messages[0].message == doc.summary)) {
                item {
                    ChatWelcomeBox(lang = lang)
                }
            }

            items(messages) { msg ->
                // Omitimos o resumo redundante na conversa se for idêntico
                if (msg.role == "model" && msg.message == doc.summary) {
                    ChatBubble(
                        message = Loc.get("chat_welcome_desc", lang),
                        isUser = false
                    )
                } else {
                    ChatBubble(message = msg.message, isUser = msg.role == "user")
                }
            }

            if (isLoading) {
                item {
                    ThinkingModelBubble(lang = lang)
                }
            }
        }

        // Caixa de Entrada de Mensagens no rodapé
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { 
                        Text(
                            if (isApiKeyConfigured) Loc.get("ask_placeholder", lang) else "IA Offline..."
                        ) 
                    },
                    enabled = isApiKeyConfigured && !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        if (!isApiKeyConfigured) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Config Key",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputQuery.isNotBlank() && isApiKeyConfigured && !isLoading) {
                            onSendMessage(inputQuery)
                            inputQuery = ""
                        }
                    },
                    containerColor = if (isApiKeyConfigured && !isLoading && inputQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("send_message_button")
                        .size(48.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoClassTab(
    viewModel: PdfReaderViewModel,
    lang: AppLanguage,
    tts: TextToSpeech?
) {
    val script by viewModel.videoScript.collectAsState()
    val loading by viewModel.videoLoading.collectAsState()
    val playing by viewModel.isVideoPlaying.collectAsState()

    // Slide States
    val slides by viewModel.slides.collectAsState()
    val slidesLoading by viewModel.slidesLoading.collectAsState()
    val activeSlideIdx by viewModel.activeSlideIdx.collectAsState()

    var selectedMode by remember { mutableStateOf(0) } // 0 = Videoaula, 1 = Slides do Preceptor

    val context = LocalContext.current

    // Efeito para áudio da teleaula com TTS
    DisposableEffect(playing) {
        if (playing && script.isNotEmpty() && selectedMode == 0) {
            val sections = script.split("\n")
                .filter { !it.trim().startsWith("[") && it.contains(":") }
                .map { it.substringAfter(":") }
                .take(3)
                .joinToString(". ")
            
            val readText = if (sections.isNotBlank()) sections else script.take(600)
            tts?.speak(readText, TextToSpeech.QUEUE_FLUSH, null, "VideoLesson")
        } else {
            tts?.stop()
        }
        onDispose {
            tts?.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SEGMENTED PICKER (Medina Design)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mode 0: Videoaula
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedMode = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (selectedMode == 0) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == AppLanguage.PT) "Roteiro de Aula" else "Lecture Script",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == 0) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Mode 1: Slides
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedMode = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Slideshow,
                        contentDescription = null,
                        tint = if (selectedMode == 1) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == AppLanguage.PT) "Slides de Estudo" else "Preceptor Slides",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (selectedMode == 0) {
            // VIEW: VIDEOAULA & ROTEIRO AUDIO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    Color.Black
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Loc.get("audio_lesson_title", lang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Loc.get("audio_lesson_desc", lang),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleVideoPlayback() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (playing) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0..7) {
                                AudioEqualizerBar()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (script.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Loc.get("video_intro", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.generateVideoScriptForSelectedDocument() },
                            enabled = !loading,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Loc.get("generate_script", lang))
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Loc.get("script_title", lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val lines = script.split("\n")
                        for (line in lines) {
                            if (line.isBlank()) continue
                            if (line.trim().startsWith("[")) {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp)
                                )
                            } else {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // VIEW: PRESENTATION SLIDE DECK
            if (slidesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) "Estruturando slides clínicos..." else "Drafting clinical illustrations...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (slides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) 
                                "Gere uma apresentação de slides de preceptor universitário completa com pontos anatômicos e fisiopatologia rica baseada no nível atual de estudo." 
                            else 
                                "Formulate a personalized academic slide presentation covering clinical pathology rules, structured to match your university track.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.generateSlidesForSelectedDocument() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (lang == AppLanguage.PT) "Gerar Slides do Assunto" else "Assemble Slides")
                        }
                    }
                }
            } else {
                val slide = slides.getOrNull(activeSlideIdx)
                if (slide != null) {
                    // SLIDE FRAME
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            // Slide Header bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == AppLanguage.PT) "SLIDE ACADÊMICO" else "ACADEMIC SLIDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "Slide ${activeSlideIdx + 1} / ${slides.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Slide Title
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bullet points
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val bulletLines = slide.content.split("\n")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                bulletLines.forEach { bulletText ->
                                    val cleanText = bulletText.trim().removePrefix("-").trim()
                                    if (cleanText.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.DoubleArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = cleanText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Illustration recommendation
                            if (slide.visualPrompt.isNotEmpty()) {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Landscape,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.PT) "Esboço Ilustrativo Mapeado:" else "Mapped Medical Vector / Illustration:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = slide.visualPrompt,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SLIDE CONTROLS (Prev / Next Grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.prevSlide() },
                            enabled = activeSlideIdx > 0,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == AppLanguage.PT) "Anterior" else "Previous")
                        }

                        // Progress Indicator dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            slides.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == activeSlideIdx) 8.dp else 4.dp)
                                        .background(
                                            color = if (idx == activeSlideIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.nextSlide() },
                            enabled = activeSlideIdx < slides.size - 1,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (lang == AppLanguage.PT) "Próximo" else "Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }

                    // Botão rápido para Regenerar / Re-adaptar caso subam de nível
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { viewModel.generateSlidesForSelectedDocument() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PT) "Atualizar Slides para o Nível Atual" else "Update Slides to Current Level",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioEqualizerBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (400..800).random(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "height"
    )

    Box(
        modifier = Modifier
            .width(6.dp)
            .height(heightScale.dp)
            .background(Color.White, shape = RoundedCornerShape(50.dp))
    )
}

@Composable
fun SmartQuizTab(
    viewModel: PdfReaderViewModel,
    lang: AppLanguage
) {
    val list by viewModel.quizQuestions.collectAsState()
    val loading by viewModel.quizLoading.collectAsState()
    val activeIdx by viewModel.activeQuestionIdx.collectAsState()
    val selectedAns by viewModel.selectedAnswerIdx.collectAsState()
    val showExpl by viewModel.showExplanation.collectAsState()
    val score by viewModel.quizScore.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Loc.get("quiz_intro", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.generateQuizForSelectedDocument() },
                        enabled = !loading,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(imageVector = Icons.Default.Quiz, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Loc.get("generate_quiz", lang))
                        }
                    }
                }
            }
        } else {
            val q = list.getOrNull(activeIdx)
            if (q != null) {
                // Progresso e Dificuldade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Loc.get("question_label", lang)} ${activeIdx + 1} ${Loc.get("of", lang)} ${list.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    // Badge de dificuldade baseado na progressão gradual
                    val badgeColor = when (q.difficulty) {
                        "Fácil", "Easy" -> Color(0xFF2E7D32)
                        "Médio", "Medium" -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50.dp))
                            .border(1.dp, badgeColor, shape = RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = q.difficulty,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pergunta Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Opções
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    q.options.forEachIndexed { opIdx, optionText ->
                        val isOpSelected = selectedAns == opIdx
                        val isCorrect = q.correctAnswerIdx == opIdx
                        
                        // Determinar a cor da borda e fundo se respondeu
                        val cardBg = when {
                            selectedAns == null -> MaterialTheme.colorScheme.surface
                            isCorrect -> Color(0xFFE8F5E9)                     // Correto sempre fica verde
                            isOpSelected && !isCorrect -> Color(0xFFFFEBEE)     // Errada selecionada fica vermelha
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val cardBorderColor = when {
                            selectedAns == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            isCorrect -> Color(0xFF2E7D32)
                            isOpSelected && !isCorrect -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        }

                        Card(
                            onClick = { viewModel.selectAnswer(opIdx) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .border(1.dp, cardBorderColor, shape = RoundedCornerShape(14.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (selectedAns == null) 1.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = when {
                                                selectedAns == null -> MaterialTheme.colorScheme.primaryContainer
                                                isCorrect -> Color(0xFF2E7D32)
                                                isOpSelected && !isCorrect -> Color(0xFFC62828)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedAns != null && isCorrect) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else if (selectedAns != null && isOpSelected && !isCorrect) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = "${(65 + opIdx).toChar()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedAns == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Explicação Inteligente
                AnimatedVisibility(visible = showExpl) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (selectedAns == q.correctAnswerIdx) Loc.get("correct", lang) else "${Loc.get("incorrect", lang)} ${(65 + q.correctAnswerIdx).toChar()}",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAns == q.correctAnswerIdx) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Ação de Próxima Pergunta
                if (selectedAns != null) {
                    Button(
                        onClick = { viewModel.nextQuizQuestion() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(Loc.get("next_question", lang))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                // FIM DO QUIZ - EXIBIR RESULTADO FINAL
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Troféu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = Loc.get("quiz_completed", lang),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${Loc.get("your_score", lang)} $score / ${list.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.restartQuiz() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Loc.get("restart", lang))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatWelcomeBox(lang: AppLanguage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Brain ready",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Loc.get("chat_welcome_title", lang),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Loc.get("chat_welcome_desc", lang),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    val containerBg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(containerBg, shape = bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ThinkingModelBubble(lang: AppLanguage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 240.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Loc.get("thinking", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Função utilitária para obter o nome real do arquivo PDF a partir da Uri de seleção do sistema.
 */
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "documento.pdf"
}

@Composable
fun Tutor3DTab(
    viewModel: PdfReaderViewModel,
    lang: AppLanguage,
    tts: TextToSpeech?
) {
    val tutorStyle by viewModel.tutorLearningStyle.collectAsState()
    val tutorState by viewModel.tutorState.collectAsState()
    val tutorMessages by viewModel.tutorMessages.collectAsState()
    val empathyLevel by viewModel.tutorEmpathyLevel.collectAsState()

    var userText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Infinite transitions for live 3D tutor animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = if (tutorState == "THINKING" || tutorState == "SPEAKING") 0.95f else 0.98f,
        targetValue = if (tutorState == "THINKING" || tutorState == "SPEAKING") 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (tutorState == "SPEAKING") 1200 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotateCircle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Equalizer bar heights
    val barHeight1 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val barHeight2 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val barHeight3 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER: PERSONALIZED COPILOT TITLE CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (lang == AppLanguage.PT) "🧠 COGNITIVIDADE MÉDICA INTEGRADA" else "🧠 INTEGRATED MEDICAL COGNITION",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == AppLanguage.PT) "O SinapseDoc analisa como você aprende e molda a matéria à sua linha de raciocínio lógico." 
                           else "SinapseDoc analyzes how you learn and shapes study materials to match your line of logical reasoning.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INTERACTIVE 3D AVATAR CARD WITH DYNAMIC EFFECTS
        Box(
            modifier = Modifier
                .size(230.dp)
                .drawBehind {
                    // Radial background glow matching tutor status
                    val glowColor = when (tutorState) {
                        "THINKING" -> Color(0xFF3B82F6).copy(alpha = 0.25f)
                        "SPEAKING" -> Color(0xFF10B981).copy(alpha = 0.3f)
                        else -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            radius = this.size.width / 1.1f
                        ),
                        radius = this.size.width / 1.3f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Rotating dashed circle if thinking/loading
            if (tutorState == "THINKING") {
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = this.size.width / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 25f), 0f)
                        )
                    )
                }
            }

            // Outer pulse circle
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .scale(scalePulse)
                    .background(
                        color = when (tutorState) {
                            "THINKING" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            "SPEAKING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Image layer
                Image(
                    painter = painterResource(id = R.drawable.img_tutor_3d),
                    contentDescription = "Professor Virtual 3D",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.linearGradient(
                                colors = when (tutorState) {
                                    "THINKING" -> listOf(Color(0xFF3B82F6), Color(0xFF93C5FD))
                                    "SPEAKING" -> listOf(Color(0xFF10B981), Color(0xFF6EE7B7))
                                    else -> listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outlineVariant)
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Small live equalizer bar chart icon overlay if speaking
            if (tutorState == "SPEAKING") {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-12).dp, y = (-12).dp)
                        .background(Color(0xFF10B981), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.width(3.dp).height(barHeight1.dp).background(Color.White, shape = CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(barHeight2.dp).background(Color.White, shape = CircleShape))
                    Box(modifier = Modifier.width(3.dp).height(barHeight3.dp).background(Color.White, shape = CircleShape))
                }
            }
        }

        // TUTOR COGNITIVE SUB-LABEL / SPEECH SYNTH STATUS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when (tutorState) {
                            "THINKING" -> Color(0xFF3B82F6)
                            "SPEAKING" -> Color(0xFF10B981)
                            else -> Color(0xFFFF9800)
                        },
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (tutorState) {
                    "THINKING" -> if (lang == AppLanguage.PT) "Modelando raciocínio..." else "Modeling reasoning..."
                    "SPEAKING" -> if (lang == AppLanguage.PT) "Explicando em áudio..." else "Vocalizing answer..."
                    else -> if (lang == AppLanguage.PT) "SinapseDoc Online & Atento" else "SinapseDoc Online & Ready"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when (tutorState) {
                    "THINKING" -> Color(0xFF3B82F6)
                    "SPEAKING" -> Color(0xFF10B981)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CONNECTION / EMPATHY INTERACTIVE BAR WITH GAMIFIED GOALS
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PT) "🤝 Conexão de Aprendizado" else "🤝 Academic Rapport",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$empathyLevel%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = empathyLevel / 100f,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        empathyLevel < 50 -> if (lang == AppLanguage.PT) "Sintonia Inicial: Continue conversando para o tutor entender seus padrões de erro!" else "Rapport Initial: Talk more so the preceptor learns your error patterns!"
                        empathyLevel < 80 -> if (lang == AppLanguage.PT) "Frequência Acoplada: O preceptor já compreende suas principais dificuldades teóricas." else "Coupled Frequency: Teacher is already adjusting to your learning habits!"
                        else -> if (lang == AppLanguage.PT) "Empatia Plena: Sintonia absoluta de aprendizado. Explicação sob medida!" else "Synthesized Bond: Maximum empathetic sync. Answers are tailor-made!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // COGNITIVE LEARNING STYLE CHIPS - LARGE AND EXPLICIT
        Text(
            text = if (lang == AppLanguage.PT) "SELECIONE SEU ESTILO DE RACIOCÍNIO:" else "SELECT YOUR REASONING STYLE:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp, start = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )

        val stylesList = listOf(
            Triple("visual", "🗺️ " + if (lang == AppLanguage.PT) "Visual / Esquemático" else "Visual / Diagrams", if (lang == AppLanguage.PT) "Analogias mentais e diagramas visuais." else "Mental images & maps."),
            Triple("conceptual", "📚 " + if (lang == AppLanguage.PT) "Teórico Profundo" else "Academic Theory", if (lang == AppLanguage.PT) "Rigor científico e caminhos biológicos." else "Advanced pathways & textbooks."),
            Triple("practical", "🩺 " + if (lang == AppLanguage.PT) "Casos de Prova" else "Clinical Cases", if (lang == AppLanguage.PT) "Beira de leito, anamnese e condutas reais." else "Ward tips & exam preparation."),
            Triple("mnemonic", "🧠 " + if (lang == AppLanguage.PT) "Mnemônicos & Hacks" else "Memory Shortcuts", if (lang == AppLanguage.PT) "Siglas, truques rápidos e memorização." else "Acronyms & rapid recall active study.")
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stylesList.forEach { item ->
                val isSelected = tutorStyle == item.first
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) 
                                         else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setTutorLearningStyle(item.first)
                            // Conversa instantaneamente
                            val quickGravel = when (item.first) {
                                "visual" -> if (lang == AppLanguage.PT) "Modo de raciocínio Visual ativo! Agora usarei muitas metáforas e diagramas espaciais para desenhar a fisiopatologia no seu cérebro. O que quer esquematizar?" else "Visual thinking style active! I'll now use visual maps and rich spatial metaphors."
                                "conceptual" -> if (lang == AppLanguage.PT) "Modo Teórico Avançado ativo! Foco em correlações acadêmicas, Robbins, Guyton e detalhes moleculares profundos. Qual assunto minucioso quer desvendar?" else "Advanced Theory active! Focus on physiology pathways & cellular biology."
                                "practical" -> if (lang == AppLanguage.PT) "Parâmetro Prático de Beira-leito ativado! Vamos discutir o diagnóstico diferencial, as indicações de terapia e condutas que caem em provas. Qual o caso de hoje?" else "Bedside Clinical practice mode active! Let's talk diagnostics and therapeutics."
                                else -> if (lang == AppLanguage.PT) "Mnemônicos e Recordação Ativa ligados! Vamos simplificar o complexo com siglas inteligentes, listas diretas e hacks rápidos. Qual matéria quer memorizar fácil?" else "Study hacks and mnemonics active! Let's simplify and summarize rapidly."
                            }
                            viewModel.setTutorState("SPEAKING")
                            tts?.stop()
                            tts?.speak(quickGravel, TextToSpeech.QUEUE_FLUSH, null, "QuickGravel")
                        }
                        .testTag("style_chip_" + item.first)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { 
                                viewModel.setTutorLearningStyle(item.first)
                                val quickGravel = when (item.first) {
                                    "visual" -> if (lang == AppLanguage.PT) "Modo de raciocínio Visual ativo! Agora usarei muitas metáforas e diagramas espaciais para desenhar a fisiopatologia no seu cérebro. O que quer esquematizar?" else "Visual thinking style active! I'll now use visual maps and rich spatial metaphors."
                                    "conceptual" -> if (lang == AppLanguage.PT) "Modo Teórico Avançado ativo! Foco em correlações acadêmicas, Robbins, Guyton e detalhes moleculares profundos. Qual assunto minucioso quer desvendar?" else "Advanced Theory active! Focus on physiology pathways & cellular biology."
                                    "practical" -> if (lang == AppLanguage.PT) "Parâmetro Prático de Beira-leito ativado! Vamos discutir o diagnóstico diferencial, as indicações de terapia e condutas que caem em provas. Qual o caso de hoje?" else "Bedside Clinical practice mode active! Let's talk diagnostics and therapeutics."
                                    else -> if (lang == AppLanguage.PT) "Mnemônicos e Recordação Ativa ligados! Vamos simplificar o complexo com siglas inteligentes, listas diretas e hacks rápidos. Qual matéria quer memorizar fácil?" else "Study hacks and mnemonics active! Let's simplify and summarize rapidly."
                                }
                                viewModel.setTutorState("SPEAKING")
                                tts?.stop()
                                tts?.speak(quickGravel, TextToSpeech.QUEUE_FLUSH, null, "QuickGravel")
                            }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = item.second,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.third,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DISCUSSION BUBBLES WINDOW / CHAT DIALOG STREAM
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PT) "💬 Interação Acadêmica Recente" else "💬 Recent Academic Exchange",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.clearTutorChat() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (tutorMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PT) "Olá! Escolha o formato em que prefere aprender, toque em um dos tópicos rápidos abaixo ou mande sua dúvida!" 
                                   else "Hello! Choose your desired learning style, select a quick topic below or type your custom medical question!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Show last 4 messages in custom design bubbles
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tutorMessages.takeLast(4).forEach { msg ->
                            val isModel = msg.role == "model"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isModel) Alignment.Start else Alignment.End
                            ) {
                                Text(
                                    text = if (isModel) "SinapseDoc 🎓" else "Você 🧑‍🎓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isModel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isModel) 0.dp else 12.dp,
                                                bottomEnd = if (isModel) 12.dp else 0.dp
                                            )
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SUGGESTION AND QUICK DICTION CARDS - TAILORED DYNAMICALLY
        Text(
            text = if (lang == AppLanguage.PT) "💡 TÓPICOS ENCORAJADORES RÁPIDOS" else "💡 QUICK LEARNING COMMANDS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val quickTopics = listOf(
            if (lang == AppLanguage.PT) "Crie uma analogia simples e lúdica para o assunto" else "Explain with a funny simple metaphor",
            if (lang == AppLanguage.PT) "Quais as pegadinhas e armadilhas comuns em provas?" else "What are common traps in medical exams on this?",
            if (lang == AppLanguage.PT) "Crie um mnemônico rápido do conteúdo" else "Make a quick clinical mnemonic",
            if (lang == AppLanguage.PT) "Simule um caso de consultório real para mim agora" else "Simulate a real emergency room case study"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickTopics) { query ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable {
                            viewModel.sendMessageTo3DTutor(query, lang) { response ->
                                tts?.stop()
                                tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "TutorResponse")
                            }
                        }
                ) {
                    Text(
                        text = query,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // INTERACTIVE VOCAL CHAT CONTROLLER / INPUT BOX
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userText,
                onValueChange = { userText = it },
                placeholder = {
                    Text(
                        if (lang == AppLanguage.PT) "Mande sua dúvida para o Tutor..." 
                        else "Ask your virtual professor..."
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .testTag("tutor_custom_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Submit / Converse button
            FloatingActionButton(
                onClick = {
                    if (userText.trim().isNotEmpty()) {
                        val queryMsg = userText
                        userText = ""
                        viewModel.sendMessageTo3DTutor(queryMsg, lang) { response ->
                            tts?.stop()
                            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "TutorCustomSpeech")
                        }
                    }
                },
                modifier = Modifier.size(50.dp).testTag("tutor_submit_button"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Converse")
            }
        }

        // CONTROL ROW: Pause TTS capability explicitly to ensure absolute responsive behavior
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            if (tutorState == "SPEAKING") {
                Button(
                    onClick = {
                        tts?.stop()
                        viewModel.setTutorState("IDLE")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == AppLanguage.PT) "Silenciar Tutor" else "Mute Virtual Avatar")
                }
            } else {
                val lastResponse = tutorMessages.lastOrNull { msg -> msg.role == "model" }
                if (lastResponse != null) {
                    Button(
                        onClick = {
                            viewModel.setTutorState("SPEAKING")
                            tts?.stop()
                            tts?.speak(lastResponse.message, TextToSpeech.QUEUE_FLUSH, null, "TutorReplay")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == AppLanguage.PT) "Ouvir Explicação" else "Listen Explanation")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: PdfReaderViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val isDark by viewModel.darkThemeEnabled.collectAsState()
    val voiceGender by viewModel.tutorVoiceGender.collectAsState()
    val voiceSpeed by viewModel.tutorVoiceSpeed.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lang == AppLanguage.PT) "Configurações Globais" 
                           else if (lang == AppLanguage.ES) "Configuraciones Globales" 
                           else "Global Settings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TEMA ESCURO
                Column {
                    Text(
                        text = if (lang == AppLanguage.PT) "Visual e Tema" 
                               else if (lang == AppLanguage.ES) "Visual y Tema" 
                               else "Visual & Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (lang == AppLanguage.PT) "Tema Escuro" 
                                               else if (lang == AppLanguage.ES) "Tema Oscuro" 
                                               else "Dark Theme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.PT) "Ideal para estudos noturnos" 
                                               else "Ideal for late night studying",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isDark,
                                onCheckedChange = { viewModel.setDarkThemeEnabled(it) }
                            )
                        }
                    }
                }

                // 2. IDIOMA DO APLICATIVIVO
                Column {
                    Text(
                        text = if (lang == AppLanguage.PT) "Idioma do Sistema" 
                               else if (lang == AppLanguage.ES) "Idioma del Sistema" 
                               else "System Language",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val languages = listOf(
                                Triple(AppLanguage.PT, "Português 🇧🇷", "Aulas em português"),
                                Triple(AppLanguage.EN, "English 🇺🇸", "Study in English"),
                                Triple(AppLanguage.ES, "Español 🇪🇸", "Clases en español")
                            )
                            languages.forEach { (langObj, nameText, descText) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.changeLanguage(langObj) }
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentLang == langObj,
                                        onClick = { viewModel.changeLanguage(langObj) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(nameText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(descText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. IDENTIDADE E INTENSIDADE DA VOZ DO TUTOR
                Column {
                    Text(
                        text = if (lang == AppLanguage.PT) "Identidade de Voz do Tutor" 
                               else if (lang == AppLanguage.ES) "Identidad de Voz del Tutor" 
                               else "Tutor Voice Profile",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val genders = listOf(
                                Triple("female", "Dra. Sofia 👩‍⚕️ (Feminina)", if (lang == AppLanguage.PT) "Voz clara para explicações e neuro-anatomia" else "Clear female tone for pathology overview"),
                                Triple("male", "Dr. Gabriel 👨‍⚕️ (Masculina)", if (lang == AppLanguage.PT) "Voz firme para condutas e casos clínicos" else "Deep male tone for clinical advice")
                            )
                            genders.forEach { (genderKey, nameText, descText) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setTutorVoiceGender(genderKey) }
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = voiceGender == genderKey,
                                        onClick = { viewModel.setTutorVoiceGender(genderKey) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(nameText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(descText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. VELOCIDADE DA FALA DO TUTOR
                Column {
                    Text(
                        text = if (lang == AppLanguage.PT) "Velocidade de Fala" 
                               else if (lang == AppLanguage.ES) "Velocidad de Habla" 
                               else "Speech Velocity",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val speeds = listOf(
                                    Triple(0.85f, "0.85x", if (lang == AppLanguage.PT) "Lenta" else "Slow"),
                                    Triple(1.0f, "1.0x", if (lang == AppLanguage.PT) "Normal" else "Normal"),
                                    Triple(1.22f, "1.25x", if (lang == AppLanguage.PT) "Rápida" else "Fast")
                                )
                                speeds.forEach { (speedValue, labelText, descText) ->
                                    Card(
                                        onClick = { viewModel.setTutorVoiceSpeed(speedValue) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (voiceSpeed == speedValue) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                             else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                            .border(
                                                width = 1.dp,
                                                color = if (voiceSpeed == speedValue) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(labelText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(descText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(if (lang == AppLanguage.PT) "Concluir" else if (lang == AppLanguage.ES) "Listo" else "Done")
            }
        }
    )
}

