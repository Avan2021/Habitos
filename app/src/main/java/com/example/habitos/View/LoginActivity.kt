package com.example.habitos.View

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.habitos.Model.UserData
import com.example.habitos.databinding.ActivityLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("Users")

        binding.myloginButton.setOnClickListener {
            val loginuserName = binding.myloginUsername.text.toString()
            val loginuserPasssword = binding.myloginPassword.text.toString()
            if(loginuserName.isNotEmpty() && loginuserPasssword.isNotEmpty()){
                loginUser(loginuserName,loginuserPasssword)
            }
            else{
                Toast.makeText(this@LoginActivity, "These fields are manadatory", Toast.LENGTH_SHORT).show()
            }
        }
        binding.myloginTextview.setOnClickListener {
            startActivity(Intent(this@LoginActivity,SignupActivity::class.java))
            finish()
        }

    }



    private fun loginUser(username:String, userpassword:String){
        databaseReference.orderByChild("userName").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(datasnapshot: DataSnapshot) {
                if(datasnapshot.exists()){
                    for(userSnapshot in datasnapshot.children){
                        val userData = userSnapshot.getValue(UserData::class.java)

                        if(userData!=null && userData.userpassword == userpassword){
                            Toast.makeText(this@LoginActivity, "Login Successfull", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                            return
                        }
                    }
                }else{
                    Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}