package com.example.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class AuthRepository(
    auth: FirebaseAuth = Firebase.auth
) : AuthenticationRepositoryImpl(auth)

