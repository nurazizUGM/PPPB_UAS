package com.example.ppapb_uas.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.ppapb_uas.models.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getById(id: String): Product?

    @Insert
    fun insert(product: Product)

    @Delete
    fun delete(product: Product)
}