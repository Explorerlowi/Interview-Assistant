package com.example.interviewassistant.di

import com.example.interviewassistant.core.database.DatabaseDriverFactory
import com.example.interviewassistant.core.network.ApiClient
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.DefaultDispatcherProvider
import com.example.interviewassistant.database.InterviewDatabase
import com.example.interviewassistant.feature.interviewassistant.data.local.SettingsProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.data.local.SqlDelightInterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.data.local.SqlDelightResumeRepository
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.PaddleOcrGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.PaddleOcrRemoteDataSource
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.OpenAiStreamGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.OpenAiStreamRemoteDataSource
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiSpeechRemoteDataSource
import com.example.interviewassistant.feature.interviewassistant.data.repository.ContinuousSpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ProviderSettingsViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ResumeLibraryViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.InterviewSessionViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.SessionHistoryViewModel
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewAnswerGenerator
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewPromptBuilder
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.PrivacyRedactor
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ProviderConnectionTester
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ResumeOcrCoordinator
import com.example.interviewassistant.feature.login.data.local.InMemoryLoginLocalDataSource
import com.example.interviewassistant.feature.login.data.local.LoginLocalDataSource
import com.example.interviewassistant.feature.login.data.remote.LoginRemoteDataSource
import com.example.interviewassistant.feature.login.data.remote.MockLoginRemoteDataSource
import com.example.interviewassistant.feature.login.data.repository.LoginRepositoryImpl
import com.example.interviewassistant.feature.login.domain.repository.LoginRepository
import com.example.interviewassistant.feature.login.domain.usecase.LoginUseCase
import com.example.interviewassistant.feature.login.presentation.viewmodel.LoginViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule(), platformModule())
}

/** Called by iOS / Desktop when no extra declaration is needed. */
fun initKoin() = initKoin {}

fun appModule() = module {
    single { ApiClient() }
    single<CoroutineDispatcherProvider> { DefaultDispatcherProvider() }
    single { InterviewDatabase(get<DatabaseDriverFactory>().create()) }
    single<ProviderConfigurationRepository> {
        SettingsProviderConfigurationRepository(get(), get())
    }
    single<ResumeRepository> { SqlDelightResumeRepository(get(), get(), get()) }
    single<InterviewSessionRepository> { SqlDelightInterviewSessionRepository(get(), get(), get()) }
    single<PaddleOcrGateway> { PaddleOcrRemoteDataSource(get<ApiClient>().httpClient) }
    single<OpenAiStreamGateway> { OpenAiStreamRemoteDataSource(get<ApiClient>().httpClient) }
    single<XunfeiSpeechGateway> {
        XunfeiSpeechRemoteDataSource(get<ApiClient>().httpClient, get())
    }
    single<SpeechRecognizer> { ContinuousSpeechRecognizer(get(), get(), get()) }
    single { InterviewPromptBuilder() }
    single { PrivacyRedactor() }
    factory {
        ProviderConnectionTester(
            client = get<ApiClient>().httpClient,
            providers = get(),
            xunfeiAuthUrlFactory = get(),
            llmGateway = get(),
        )
    }
    factory { InterviewAnswerGenerator(get(), get(), get(), get()) }
    factory { ResumeOcrCoordinator(get(), get(), get(), get()) }
    factory { ProviderSettingsViewModel(get(), get(), get()) }
    factory { ResumeLibraryViewModel(get(), get(), get(), get()) }
    factory { InterviewSessionViewModel(get(), get(), get(), get(), get()) }
    factory { SessionHistoryViewModel(get(), get(), get()) }

    single<LoginRemoteDataSource> { MockLoginRemoteDataSource() }
    single<LoginLocalDataSource> { InMemoryLoginLocalDataSource() }
    single<LoginRepository> { LoginRepositoryImpl(get(), get()) }
    factory { LoginUseCase(get()) }
    factory { LoginViewModel(get(), get()) }
}

expect fun platformModule(): org.koin.core.module.Module
