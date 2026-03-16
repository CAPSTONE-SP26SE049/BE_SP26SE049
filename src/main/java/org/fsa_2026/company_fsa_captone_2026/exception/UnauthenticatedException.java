package com.example.starter_project_2025.exception;

public class UnauthenticatedException extends RuntimeException
{
    public UnauthenticatedException(String message)
    {
        super(message);
    }
}
