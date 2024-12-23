package com.example.fitplate.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import android.widget.Toast

import com.example.fitplate.AuthManager
import com.example.fitplate.R
import com.example.fitplate.RealtimeDatabase
import com.example.fitplate.calculators.BadanProgressTracker
import com.example.fitplate.databinding.HomeActivityBinding
import java.text.*
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: HomeActivityBinding

    private val dbTargetGizi by lazy { RealtimeDatabase.instance().getReference("TargetGiziHarian") }
    private val dbProgresGizi by lazy { RealtimeDatabase.instance().getReference("ProgressGiziHarian") }

    private lateinit var authManager: AuthManager

    // variabel user dari database

    // variabel targetGizi dari database
    private var targetCalorie: Double? = null
    private var targetProtein: Double? = null
    private var targetKarbo: Double? = null
    private var targetLemak: Double? = null

    // variabel progresGizi dari database
    private var progressCalorie: Double? = null
    private var progressProtein: Double? = null
    private var progressKarbo: Double? = null
    private var progressLemak: Double? = null

    private val decimalFormat = DecimalFormat("#.0") // Satu angka di belakang koma

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        // Inisialisasi binding
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AuthManager
        authManager = AuthManager(this)

        // Get user ID
        val userId = authManager.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadData(userId)

        // Atur listener
        setupClickListeners()
    }

    // fungsi untuk menampilkan data
    private var isGiziTargetLoaded = false
    private var isGiziProgressLoaded = false
    private var isAirTargetLoaded = false
    private var isAirProgressLoaded = false
    private var isBadanProgressLoaded = false

    private var fetchedProgressData: Map<String, Double> = mapOf()

    private fun loadData(userId: String) {
        val dateKey = getCurrentDateKey()

        // Fetch target nutrition
        dbTargetGizi.child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                Log.d("FirebaseDebug", "userId: $userId")
                targetCalorie = snapshot.child("targetKalori").value?.toString()?.toDoubleOrNull()
                targetProtein = snapshot.child("targetProtein").value?.toString()?.toDoubleOrNull()
                targetKarbo = snapshot.child("targetKarbohidrat").value?.toString()?.toDoubleOrNull()
                targetLemak = snapshot.child("targetLemak").value?.toString()?.toDoubleOrNull()

                Log.d("FirebaseDebug", "Target Lemak: $targetLemak")

                //Toast.makeText(this@HomeActivity, "user data fetched", Toast.LENGTH_SHORT).show()
            } else {
                // Additional data does not exist, navigate to InputUserActivity
                Toast.makeText(this@HomeActivity, "Datamu tidak ketemu", Toast.LENGTH_SHORT).show()
            }
            isGiziTargetLoaded = true
            checkDataLoaded()
        }

        // Fetch progress nutrition
        dbProgresGizi.child(userId).child(dateKey).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {

                // Fetch user inputs from the database
                progressCalorie = snapshot.child("jumlahKalori").value?.toString()?.toDoubleOrNull()
                progressProtein = snapshot.child("jumlahProtein").value?.toString()?.toDoubleOrNull()
                progressKarbo = snapshot.child("jumlahKarbohidrat").value?.toString()?.toDoubleOrNull()
                progressLemak = snapshot.child("jumlahLemak").value?.toString()?.toDoubleOrNull()
                //Toast.makeText(this@HomeActivity, "Gizi data fetched", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@HomeActivity, "Wah kamu belum ada progres nih, ayo mulai!", Toast.LENGTH_SHORT).show()
            }
            isGiziProgressLoaded = true
            checkDataLoaded()
        }

        val progressTracker = BadanProgressTracker()
        progressTracker.fetchUserData(userId, object : BadanProgressTracker.ProgressCallback {
            override fun onSuccess(data: Map<String, Double>) {
                fetchedProgressData = data
                isBadanProgressLoaded = true
                checkDataLoaded()
            }
            override fun onFailure(errorMessage: String) {
                Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkDataLoaded() {
        if (isGiziTargetLoaded && isGiziProgressLoaded) {
            updateNutritionUI()
        } else if (isAirTargetLoaded && isAirProgressLoaded) {
            //updateAirUI()
        }
        if (isBadanProgressLoaded && fetchedProgressData.isNotEmpty()) {
            updateBbPreview(fetchedProgressData)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateNutritionUI() {
        binding.apply {
            textViewKaloriTarget.text = "${decimalFormat.format(progressCalorie ?: 0.0)}/${decimalFormat.format(targetCalorie ?: 0.0)} Kkal"
            textViewProteinTarget.text = "${decimalFormat.format(progressProtein ?: 0.0)}/${decimalFormat.format(targetProtein ?: 0.0)} Kkal"
            textViewKarboTarget.text = "${decimalFormat.format(progressKarbo ?: 0.0)}/${decimalFormat.format(targetKarbo ?: 0.0)} Kkal"
            textViewLemakTarget.text = "${decimalFormat.format(progressLemak ?: 0.0)}/${decimalFormat.format(targetLemak ?: 0.0)} Kkal"

            progressBarKalori.progress = calculateProgress(progressCalorie, targetCalorie)
            progressBarProtein.progress = calculateProgress(progressProtein, targetProtein)
            progressBarKarbo.progress = calculateProgress(progressKarbo, targetKarbo)
            progressBarLemak.progress = calculateProgress(progressLemak, targetLemak)
        }
    }

    private fun updateBbPreview(data: Map<String, Double>) {
        val weight = data["beratBadan"] ?: 0.0
        val targetWeight = data["targetBb"] ?: 0.0
        Log.d("UIUpdate", "Updating UI: weight = $weight, targetWeight = $targetWeight")
        binding.tvPreviewBb.text = "$weight kg/ $targetWeight kg"
    }

    private fun calculateProgress(progress: Double?, target: Double?): Int {
        return if (progress != null && target != null && target > 0) {
            ((progress / target) * 100).toInt()
        } else {
            0
        }
    }

    private fun getCurrentDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun setupClickListeners() {

        binding.cardViewDietRecipe.setOnClickListener{
            navigateToActivity(RecipeActivity::class.java)
        }

        binding.cardViewMengukurTubuh.setOnClickListener {
            navigateToActivity(MengukurTubuhActivity::class.java)
        }

        binding.cardViewFoodTracker.setOnClickListener {
            navigateToActivity(FoodJournalActivity::class.java)
        }

        binding.profileButton.setOnClickListener {
            navigateToActivity(ProfilActivity::class.java)
        }

        binding.cardViewMedalGuide.setOnClickListener {
            navigateToActivity(GuideActivity::class.java)
        }

        binding.leaderboardButton.setOnClickListener {
            //navigateToActivity(LeaderboardActivity::class.java)
            goToLeaderboard()
        }
    }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    // Fungsi untuk berpindah ke LeaderboardActivity
    private fun goToLeaderboard() {
        //val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
    }
}
