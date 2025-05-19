package com.example.ddma.data.model

import com.example.ddma.data.model.PastelesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import retrofit2.HttpException

class PastelesRepository(private val apiService: ApiService) {
    suspend fun getPasteles(): RepositoryResult<List<PastelesResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPasteles()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        RepositoryResult.Success(body)
                    } else {
                        RepositoryResult.Error(Exception("Empty response body"))
                    }
                } else {
                    RepositoryResult.Error(HttpException(response))
                }
            } catch (e: IOException) {
                RepositoryResult.Error(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                RepositoryResult.Error(e)
            }
        }
    }
}