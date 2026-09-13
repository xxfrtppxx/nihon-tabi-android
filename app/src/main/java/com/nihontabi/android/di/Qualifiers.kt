package com.nihontabi.android.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedOkHttp
