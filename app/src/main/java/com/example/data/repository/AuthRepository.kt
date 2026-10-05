package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository(
    auth: FirebaseAuth? = null
) : AuthenticationRepositoryImpl(auth)

