package by.kotlin.salesAppartment

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("listings")

    suspend fun addListing(listing: PropertyListing): String {
        val data = listing.toMap()
        val docRef = collection.add(data).await()
        return docRef.id
    }

    suspend fun updateListing(firestoreId: String, listing: PropertyListing) {
        collection.document(firestoreId).set(listing.toMap()).await()
    }

    suspend fun deleteListing(firestoreId: String) {
        collection.document(firestoreId).delete().await()
    }

    suspend fun getAllListings(): List<PropertyListing> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PropertyListing::class.java)?.copy(firestoreId = doc.id)
        }
    }

    fun observeListings(listener: com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot>) =
        collection.addSnapshotListener(listener)

    private fun PropertyListing.toMap(): Map<String, Any?> = mapOf(
        "transactionType" to transactionType,
        "propertyType" to propertyType,
        "rooms" to rooms,
        "floor" to floor,
        "country" to country,
        "locality" to locality,
        "street" to street,
        "houseNumber" to houseNumber,
        "negotiable" to negotiable,
        "price" to price,
        "createdAt" to createdAt,
        "latitude" to latitude,
        "longitude" to longitude,
        "imageUrls" to imageUrls,
        "userId" to userId
    )
}