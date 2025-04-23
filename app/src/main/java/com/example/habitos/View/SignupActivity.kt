package com.example.habitos.View

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.habitos.Model.UserData
import com.example.habitos.databinding.ActivitySignupBinding

import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("Users")

        binding.mysignupButton.setOnClickListener {
            val userName = binding.mysignupUsername.text.toString().trim()
            val userPasssword = binding.mysignupPassword.text.toString().trim()
            if(userName.isNotEmpty() && userPasssword.isNotEmpty()){
                signupUser(userName,userPasssword)
            }
            else{
                Toast.makeText(this@SignupActivity, "These fields are manadatory", Toast.LENGTH_SHORT).show()
            }
        }
        binding.mysignupTextview.setOnClickListener {
            startActivity(Intent(this@SignupActivity,LoginActivity::class.java))
        }
    }

    private fun signupUser(userName:String, userPassword:String){
        databaseReference.orderByChild("userName").equalTo(userName).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(datasnapshot: DataSnapshot) {
                if(!datasnapshot.exists()){
                    val id = databaseReference.push().key
                    val userData = UserData(id, userName, userPassword)
                    databaseReference.child(id!!).setValue(userData)
                    Toast.makeText(this@SignupActivity, "Signed Up", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupActivity,LoginActivity::class.java))
                    finish()
                }
                else{
                    Toast.makeText(this@SignupActivity, "User already Exists", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignupActivity, "Database Error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}