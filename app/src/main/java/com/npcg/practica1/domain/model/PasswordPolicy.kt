package com.npcg.practica1.domain.model

object PasswordPolicy {
    const val MIN_LENGTH = 8

    fun isStrong(password: String): Boolean =
        password.length >= MIN_LENGTH &&
            password.any(Char::isLetter) &&
            password.any(Char::isDigit)
}
