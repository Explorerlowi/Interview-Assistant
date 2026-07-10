package com.example.interviewassistant.di

import com.example.interviewassistant.core.network.ApiClient
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.DefaultDispatcherProvider
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

    single<LoginRemoteDataSource> { MockLoginRemoteDataSource() }
    single<LoginLocalDataSource> { InMemoryLoginLocalDataSource() }
    single<LoginRepository> { LoginRepositoryImpl(get(), get()) }
    factory { LoginUseCase(get()) }
    factory { LoginViewModel(get(), get()) }
}

expect fun platformModule(): org.koin.core.module.Module
