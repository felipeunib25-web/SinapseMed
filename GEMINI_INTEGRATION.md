# 🧠 SinapseMed - Enhanced Gemini Integration

## 📋 Overview

Esta branch implementa uma integração **completa e robusta** da API Gemini no SinapseMed, transformando o app em um tutor de IA verdadeiramente inteligente com capacidades avançadas de processamento de múltiplos formatos de mídia.

## ✨ Features Implementadas

### 1. **GeminiService** - Serviço Robusto de API
- ✅ Chamadas genéricas à API com retry automático (3 tentativas)
- ✅ Cache inteligente de respostas (5 minutos de duração)
- ✅ Suporte a múltiplos modelos (gemini-1.5-flash, gemini-3.5-flash)
- ✅ Tratamento de erros granular
- ✅ Backoff exponencial para rate limiting

### 2. **TutorBrainService** - Cérebro do Tutor Online
- ✅ Personalização de aprendizado (visual, conceptual, practical, mnemonic)
- ✅ Gerenciamento de empatia (0-100%)
- ✅ Histórico conversacional persistente
- ✅ Respostas contextualizadas baseadas no nível do aluno
- ✅ Adaptação dinâmica de comunicação

### 3. **MediaProcessingService** - Processamento Multi-Formato
- ✅ **PDF**: Extração de texto completa
- ✅ **Imagens/Fotos**: OCR inteligente de anotações e slides
- ✅ **Áudio**: Transcrição de palestras e aulas gravadas
- ✅ **Vídeo**: Análise completa com truncamento seguro (14MB limit)
- ✅ Validação de arquivos

### 4. **ConversationManagementService** - Conversações Multi-Turno
- ✅ Histórico conversacional inteligente (últimas 10 mensagens)
- ✅ Sugestões automáticas de follow-up (3 níveis de dificuldade)
- ✅ Cálculo dinâmico de engajamento
- ✅ Trimming de histórico para performance

### 5. **GeminiIntegrationHelper** - Orquestrador Central
- ✅ Unificação de todos os serviços
- ✅ Pipeline completo: arquivo → processamento → resumo → IA
- ✅ Gerenciamento de contexto centralizado

## 🚀 Como Usar

### 1. **Carregar Qualquer Tipo de Arquivo**

```kotlin
// No PdfReaderViewModel
fun importFile(uri: Uri, fileName: String, fileType: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val (extractedText, metadata) = GeminiIntegrationHelper.processMediaFileWithServices(
                context = getApplication(),
                uri = uri,
                fileType = fileType, // PDF, PHOTO, AUDIO, VIDEO
                fileName = fileName
            )
            
            // Processa texto extraído...
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
        }
    }
}
```

### 2. **Interagir com o Tutor Online**

```kotlin
fun sendMessageToTutor(userMessage: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val doc = _selectedDocument.value ?: return@launch
        
        val (response, tutorResponse) = GeminiIntegrationHelper.processUserMessageWithServices(
            userMessage = userMessage,
            documentText = doc.extractedText,
            conversationHistory = _tutorMessages.value,
            userLanguage = _appLanguage.value.name,
            studentLevel = doc.userLvlProgress,
            learningStyle = _tutorLearningStyle.value
        )
        
        _tutorMessages.value = _tutorMessages.value + ChatMessage(
            role = "assistant",
            message = tutorResponse.message
        )
        
        // Atualiza empatia
        _tutorEmpathyLevel.value = minOf(100, 
            _tutorEmpathyLevel.value + tutorResponse.empathyIncrement
        )
    }
}
```

### 3. **Gerar Resumo Enriquecido**

```kotlin
val summary = GeminiIntegrationHelper.generateEnrichedSummaryWithService(
    fileName = "Cardiologia_Aula_01.pdf",
    extractedText = extractedText,
    fileType = "PDF"
)
```

## 📊 Arquitetura de Serviços

```
┌─────────────────────────────────────────┐
│    GeminiIntegrationHelper              │
│  (Orquestrador Principal)               │
└────────────┬────────────────────────────┘
             │
    ┌────────┼────────┬──────────┬─────────┐
    ▼        ▼        ▼          ▼         ▼
┌────────┐ ┌──────────────────┐ ┌──────────────┐
│Gemini  │ │   TutorBrain     │ │   Media      │
│Service │ │    Service       │ │ Processing   │
└────────┘ └──────────────────┘ └──────────────┘
    │             │                    │
    │             ▼                    │
    │     ┌──────────────────┐        │
    │     │ Conversation     │        │
    │     │ Management       │        │
    │     └──────────────────┘        │
    │             │                    │
    └─────────────┼────────────────────┘
                  ▼
            📚 Banco de Dados
           (Histórico persistente)
```

## 🔧 Configuração

### 1. **Ativar API Key**

Edite `.env`:
```env
GEMINI_API_KEY=sua_chave_aqui
```

### 2. **Dependências (já incluídas)**

```gradle
implementation(libs.retrofit)
implementation(libs.converter.moshi)
implementation(libs.pdfbox.android)
implementation(libs.firebase.ai)
```

## 📈 Performance & Otimizações

- **Cache de 5 minutos** previne requisições duplicadas
- **Histórico limitado a 10 mensagens** mantém performance
- **Retry automático** com backoff exponencial
- **Truncamento de vídeo** a 14MB (limite Gemini)
- **Processamento assíncrono** em Dispatchers.IO

## 🧪 Testes Sugeridos

```kotlin
// Teste 1: Upload de PDF e chat
fun testPdfUploadAndChat() {
    val uri = getTestPdfUri()
    importFile(uri, "test.pdf", "PDF")
    delay(3000)
    sendMessage("O que é a fisiopatologia desta doença?")
}

// Teste 2: Upload de imagem (anotações)
fun testImageOcr() {
    val photoUri = getTestPhotoUri()
    importFile(photoUri, "notes.jpg", "PHOTO")
}

// Teste 3: Interação com Tutor
fun testTutorInteraction() {
    setTutorLearningStyle("visual")
    sendMessageTo3DTutor("Explique como uma célula...") { response ->
        assert(response.isNotEmpty())
    }
}
```

## 🐛 Troubleshooting

### Erro: "Chave da API não configurada"
- Verifique `.env` tem `GEMINI_API_KEY` válida
- Rebuild o projeto: `./gradlew clean build`

### Erro: "Video file too large"
- Vídeos > 14MB são truncados automaticamente
- Use vídeos menores de 100MB idealmente

### Erro: "API Key rate limited"
- Sistema usa retry automático (3 tentativas)
- Aguarde alguns segundos antes de nova requisição

## 📝 Próximas Melhorias

- [ ] Suporte a streaming de respostas longas
- [ ] Análise de imagens com visão computacional
- [ ] Integração com Speech-to-Text nativo
- [ ] Cache persistente em banco de dados
- [ ] Analytics de engajamento do aluno
- [ ] Exportação de conversas em PDF

## 🚢 Deploy

1. Merge da branch em `main`
2. Tag de versão: `v2.0.0-enhanced-ai`
3. Build da release
4. Publicar no Play Store

## 📞 Suporte

Para dúvidas sobre integração:
- Consulte `GeminiService` para operações básicas
- Consulte `TutorBrainService` para personalizações
- Consulte `MediaProcessingService` para conversão de arquivos

---

**Status**: ✅ Pronto para Merge
**Branch**: `feature/enhanced-gemini-integration`
**Commits**: 4
**Arquivos**: 4 novos serviços
