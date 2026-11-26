package com.npcg.welliai.backend.ai

class OpenAiCredentialsException : RuntimeException()
class OpenAiModelUnavailableException : RuntimeException()
class OpenAiRateLimitedException : RuntimeException()
class OpenAiTimeoutException : RuntimeException()
class OpenAiConnectionException : RuntimeException()
class InvalidModelResponseException : RuntimeException()
class OpenAiRefusalException : RuntimeException()
